package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 专门用于 KeyStore 认证流程的文件日志记录器。
 * 日志同时写入 Android Logcat (tag: KeystoreAuth) 和文件 (keystore_auth_debug.log)。
 * 用于复现和排查锁屏密码死循环问题。
 */
object KeystoreAuthLogger {

    private const val TAG = "KeystoreAuth"
    private const val LOG_FILE_NAME = "keystore_auth_debug.log"
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var logFile: File? = null

    fun init(context: Context) {
        if (logFile == null) {
            synchronized(this) {
                if (logFile == null) {
                    logFile = File(context.filesDir, LOG_FILE_NAME)
                    info("KeystoreAuthLogger initialized", "Log file: ${logFile?.absolutePath}")
                }
            }
        }
    }

    @Synchronized
    fun info(tag: String, message: String) {
        val formatted = formatLog("INFO", tag, message)
        Log.i(TAG, formatted)
        writeToFile(formatted)
    }

    @Synchronized
    fun warning(tag: String, message: String, throwable: Throwable? = null) {
        val msg = if (throwable != null) {
            "$message | ${throwable.javaClass.simpleName}: ${throwable.message}"
        } else {
            message
        }
        val formatted = formatLog("WARN", tag, msg)
        Log.w(TAG, formatted, throwable)
        writeToFile(formatted)
    }

    @Synchronized
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val msg = if (throwable != null) {
            "$message | ${throwable.javaClass.simpleName}: ${throwable.message}"
        } else {
            message
        }
        val formatted = formatLog("ERROR", tag, msg)
        Log.e(TAG, formatted, throwable)
        writeToFile(formatted)
    }

    fun getLogFile(): File? = logFile

    private fun formatLog(level: String, tag: String, message: String): String {
        val timestamp = sdf.format(Date())
        return "$timestamp [$level] [$tag] $message"
    }

    private fun writeToFile(content: String) {
        val file = logFile ?: return
        try {
            FileWriter(file, true).use { writer ->
                writer.write("$content\n")
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log to file: ${e.message}")
        }
    }
}
