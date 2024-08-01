@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package proofcastlabs.tee

import android.content.Context
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.runBlocking
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import proofcastlabs.tee.database.DatabaseWiring
import proofcastlabs.tee.database.SQLiteHelper
import proofcastlabs.tee.security.Strongbox
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private external fun callCore(strongbox: Strongbox, db: DatabaseWiring, input: String): String

    private val WS_RETRY_DELAY = 3_000L
    private val WS_PING_INTERVAL = 55_000L

    private val verifyStateHash = BuildConfig.VERIFY_STATE_HASH.toBoolean()
    private val writeStateHash = BuildConfig.WRITE_STATE_HASH.toBoolean()
    private val isStrongboxBacked = BuildConfig.STRONGBOX_ENABLED.toBoolean()
    private var client: HttpClient? = null
    private var strongbox: Strongbox? = null
    private var db: DatabaseWiring? = null

    val TAG = "[Main]"

    init {
        System.loadLibrary("sqliteX")
        System.loadLibrary("shathree")
        System.loadLibrary("strongbox")
        Log.i(TAG, "Library loaded")
    }

    suspend fun receiveWebsocketData(context: Context) {
        client!!.webSocket(
            method = HttpMethod.Get,
            path = "/ws",
            host = "127.0.0.1",
            port = 3000,
        ) {
            Log.i(TAG, "Websocket connected")
            strongbox = Strongbox(context)
            Log.i(TAG, "Strongbox initialized")
            db = DatabaseWiring(
                context,
                SQLiteHelper(context).writableDatabase,
                verifyStateHash,
                writeStateHash
            )
            Log.i(TAG, "Database opened")
            while (true) {
                val request = incoming.receive() as? Frame.Text ?: continue
                val b64input = request.readText()
                val resp = try {
                    callCore(strongbox!!, db!!, b64input)
                } catch(e: Exception) {
                    val errMsg = "callCore failed"
                    Log.e(TAG,errMsg, e)
                    "{\"error\": \"$errMsg\"}"
                }
                send(resp)
                Log.i(TAG, "Sent!")
            }
        }
    }

    suspend fun maybeReceiveWebsocketData(context: Context) = coroutineScope {
            while (true) {
                try {
                    receiveWebsocketData(context)
                } catch (e: WebSocketException) {
                    Log.w(TAG, "Failed to connect to websocket, retrying in $WS_RETRY_DELAY seconds...")
                    delay(WS_RETRY_DELAY)
                } catch (e: Exception) {
                    // We enter here when the WS channel is closed
                    Log.w(TAG, "Coroutine exception handler, cause: ${e.message}")
                    db?.close()
                    strongbox = null
                    Log.w(TAG, "Retrying to connect")
                    delay(WS_RETRY_DELAY)
                }

            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val context = this
        client = HttpClient { install(WebSockets) { pingInterval = WS_PING_INTERVAL } }

        runBlocking {
            maybeReceiveWebsocketData(context)
        }
    }

    override fun onStop() {
        super.onStop()
        client!!.close()
        Log.i(TAG, "Websocket connection closed!")
    }
}