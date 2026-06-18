package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalletRecordWriter(private val context: Context) {

    companion object {
        private const val DIR_NAME = "SafeWallet"
        private const val FILE_NAME = "wallet_records.txt"
        private const val CRASH_FILE_NAME = "crash_logs.txt"
    }

    private val dir: File by lazy {
        val externalDir = context.getExternalFilesDir(null)
            ?: context.filesDir // fallback to internal storage
        val d = File(externalDir, DIR_NAME)
        if (!d.exists()) {
            d.mkdirs()
        }
        d
    }

    private val recordFile: File by lazy {
        File(dir, FILE_NAME)
    }

    private val crashFile: File by lazy {
        File(dir, CRASH_FILE_NAME)
    }

    /**
     * Write wallet id and name to external SD card file, one line per wallet.
     * Format: wallet_id,wallet_name
     */
    fun writeWalletRecord(walletId: String, walletName: String) {
        Log.d("WalletRecordWriter", "writeWalletRecord: $walletId,$walletName file=${recordFile.absolutePath}")
        try {
            if (!recordFile.exists()) {
                recordFile.createNewFile()
            }
            FileWriter(recordFile, true).use { writer ->
                writer.append("$walletId,$walletName\n")
                writer.flush()
            }
        } catch (e: IOException) {
            Log.d("WalletRecordWriter", "writeWalletRecord error: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Write all wallet records at once, overwriting the existing file.
     * Called on first load of wallet list from storage.
     * Format: wallet_id,wallet_name (one per line)
     */
    fun writeAllWalletRecords(records: List<Pair<String, String>>) {
        Log.d("WalletRecordWriter", "writeAllWalletRecords: count=${records.size} file=${recordFile.absolutePath}")
        try {
            FileWriter(recordFile, false).use { writer ->
                for ((id, name) in records) {
                    writer.append("$id,$name\n")
                }
                writer.flush()
            }
        } catch (e: IOException) {
            Log.d("WalletRecordWriter", "writeAllWalletRecords error: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Write crash / uncaught exception log to file.
     * Format: timestamp, thread name, exception message and full stack trace.
     */
    fun writeCrashLog(thread: Thread, throwable: Throwable) {
        try {
            if (!crashFile.exists()) {
                crashFile.createNewFile()
            }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
            val timestamp = dateFormat.format(Date())
            val sb = StringBuilder()
            sb.appendLine("========================================")
            sb.appendLine("Crash Time : $timestamp")
            sb.appendLine("Thread     : ${thread.name} (id=${thread.id})")
            sb.appendLine("Exception  : ${throwable.javaClass.name}: ${throwable.message}")
            sb.appendLine("Stack Trace:")
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            sb.appendLine(sw.toString())
            sb.appendLine("========================================")
            sb.appendLine()

            FileWriter(crashFile, true).use { writer ->
                writer.append(sb.toString())
                writer.flush()
            }
        } catch (e: IOException) {
            Log.d("WalletRecordWriter", "writeCrashLog error: ${e.message}")
            e.printStackTrace()
        }
    }
}
