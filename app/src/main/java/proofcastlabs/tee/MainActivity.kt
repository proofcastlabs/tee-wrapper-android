@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package proofcastlabs.tee

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.runBlocking
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import okhttp3.Dns
import proofcastlabs.tee.database.DatabaseWiring
import proofcastlabs.tee.database.SQLiteHelper
import proofcastlabs.tee.security.Strongbox
import java.lang.Exception
import java.net.Inet4Address
import java.net.InetAddress
import java.time.Duration

class MainActivity : AppCompatActivity() {
    private external fun callCore(strongbox: Strongbox, db: DatabaseWiring, input: String): String

    private val TAG = "[Main]"
    private val WS_RETRY_DELAY = 3_000L
    private val WS_PING_INTERVAL = 55_000L
    private val WS_DEFAULT_PORT = "3000"
    private val WS_DEFAULT_HOST = "localhost"

    private val INTENT_KEY_WS_HOST = "wsHost"
    private val INTENT_KEY_WS_PORT = "wsPort"

    private var wsHost = "localhost"
    private var wsPort = 3000
    private val verifyStateHash = BuildConfig.VERIFY_STATE_HASH.toBoolean()
    private val writeStateHash = BuildConfig.WRITE_STATE_HASH.toBoolean()
    private val isStrongboxBacked = BuildConfig.STRONGBOX_ENABLED.toBoolean()
    private var client: HttpClient? = null
    private var strongbox: Strongbox? = null
    private var db: DatabaseWiring? = null


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
            host = wsHost,
            port = wsPort
        ) {
            Log.i(TAG, "Websocket connected")
            strongbox = Strongbox(context)
            Log.i(TAG, "Strongbox initialized")
            db = DatabaseWiring(
                context,
                SQLiteHelper(context).writableDatabase,
                verifyStateHash,
                writeStateHash,
                isStrongboxBacked
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
                    e.printStackTrace()
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

        // Send intent extra by adding --es <param> to the launch.sh
        // script
        if (intent.extras != null) {
            wsHost = intent.extras!!.getString(INTENT_KEY_WS_HOST, WS_DEFAULT_HOST)
            wsPort = intent.extras!!.getString(INTENT_KEY_WS_PORT, WS_DEFAULT_PORT).toInt()
        }

        Log.d(TAG, "Host: $wsHost")
        Log.d(TAG, "Port: $wsPort")

        val dns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return Dns.SYSTEM.lookup(hostname).filter {
                    Inet4Address::class.java.isInstance(it)
                }
            }
        }

        val context = this
        val timeout = Duration.ofSeconds(100L)
        client = HttpClient(OkHttp) {
            engine { config {
                dns(dns)
                writeTimeout(timeout)
                readTimeout(timeout)
                connectTimeout(timeout)
            } }

            install(WebSockets) { pingInterval = WS_PING_INTERVAL }
        }

        runBlocking { maybeReceiveWebsocketData(context) }

    }

    override fun onStop() {
        super.onStop()
        client!!.close()
        Log.i(TAG, "Websocket connection closed!")
    }
}
