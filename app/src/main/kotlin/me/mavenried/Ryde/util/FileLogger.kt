package me.mavenried.Ryde.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {
    private const val LOG_FILE_NAME = "ryde_logs.txt"

    private fun getDateFormat() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun log(context: Context, message: String) {
        try {
            val timestamp = getDateFormat().format(Date())
            val logLine = "[$timestamp] $message\n"
            val file = File(context.filesDir, LOG_FILE_NAME)
            file.appendText(logLine)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logError(context: Context, message: String, throwable: Throwable? = null) {
        val errorMsg = buildString {
            append("ERROR: ")
            append(message)
            if (throwable != null) {
                append(" | Exception: ")
                append(throwable.localizedMessage)
                append("\n")
                append(throwable.stackTraceToString())
            }
        }
        log(context, errorMsg)
    }

    fun getLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.readText() else "No logs found."
        } catch (e: Exception) {
            "Error reading logs: ${e.localizedMessage}"
        }
    }

    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
