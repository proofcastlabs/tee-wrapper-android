package com.ptokenssentinelandroidapp

import com.ptokenssentinelandroidapp.rustlogger.RustLogger
import android.util.Log
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.ptokenssentinelandroidapp.database.DatabaseWiring
import com.ptokenssentinelandroidapp.database.SQLiteHelper
import com.ptokenssentinelandroidapp.strongbox.Strongbox


class RustBridge(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  private external fun callCore(strongbox: Strongbox, db: DatabaseWiring, input: String): String

  val context = this.reactApplicationContext.applicationContext

  val CLASS_NAME: String = "JavaRustBridge"

  var db: DatabaseWiring? = null

  var strongbox: Strongbox? = null

  init {
    System.loadLibrary("strongbox")
    System.loadLibrary("sqliteX")
    System.loadLibrary("shathree")
    this.strongbox = Strongbox(this.context)
  }

  override fun getName() = "RustBridge"

  @ReactMethod
  fun openDb(successCallback: Callback, failureCallback: Callback) {
    RustLogger.rustLog("info", this.CLASS_NAME + " opening db...")
    if (this.db != null) {
      RustLogger.rustLog("debug", this.CLASS_NAME + " db already opened")
      successCallback.invoke()
      return
    }

    try {
      val verifyStateHash = true
      val writeStateHash = true
      this.db = DatabaseWiring(
        this.context,
        SQLiteHelper(context).writableDatabase,
        verifyStateHash,
        writeStateHash
      )
      RustLogger.rustLog("info", this.CLASS_NAME + " opened SQL database")
      successCallback.invoke()
      return
    } catch (e: Exception) {
      RustLogger.rustLog("error",  this.CLASS_NAME + " error opening SQL database $e")
      failureCallback.invoke(e.message)
      return
    }
  }

  @ReactMethod
  fun closeDb(successCallback: Callback, failureCallback: Callback) {
    RustLogger.rustLog("info", this.CLASS_NAME + " closing db...")
    if (this.db == null) {
      RustLogger.rustLog("debug", this.CLASS_NAME + " db already closed")
      successCallback.invoke()
      return
    }

    try {
      SQLiteHelper(context).close()
      this.db = null
      RustLogger.rustLog("debug", this.CLASS_NAME + " closed SQL database")
      successCallback.invoke()
      return
    } catch (e: Exception) {
      RustLogger.rustLog("error", this.CLASS_NAME + " error closing SQL database $e")
      failureCallback.invoke(e.message)
      return
    }
  }

  @ReactMethod
  fun callRustCore(b64Input: String, successCallback: Callback, failureCallback: Callback) {
    try {
      RustLogger.rustLog("info", this.CLASS_NAME + " `callRustCore` successfully")
      successCallback.invoke(callCore(this.strongbox!!, this.db!!, b64Input))
      return
    } catch(e: Exception) {
      RustLogger.rustLog("error", this.CLASS_NAME + " failed to call rust core with exception: $e")
      failureCallback.invoke(e.message)
      return
    }
  }

  @ReactMethod
  fun dropDb(successCallback: Callback, failureCallback: Callback) {
    try {
      this.db!!.drop()
      RustLogger.rustLog("info", this.CLASS_NAME + " database dropped!")
      successCallback.invoke()
    } catch(e: Exception) {
      RustLogger.rustLog("error", this.CLASS_NAME + " could not drop database, it was null")
      failureCallback.invoke(e.message)
    }
  }
}
