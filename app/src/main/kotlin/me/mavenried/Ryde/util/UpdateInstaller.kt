package me.mavenried.Ryde.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateInstaller {
    private fun apkFile(context: Context) = File(context.cacheDir, "updates/ryde-update.apk")

    suspend fun download(context: Context, url: String, onProgress: (Float) -> Unit) =
        withContext(Dispatchers.IO) {
            val file = apkFile(context)
            file.parentFile?.mkdirs()
            if (file.exists()) file.delete()

            var conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true

            // Follow redirects across protocols (GitHub → CDN)
            var redirect = conn.getHeaderField("Location")
            while (redirect != null) {
                conn = URL(redirect).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 30_000
                redirect = conn.getHeaderField("Location")
            }

            val total = conn.contentLengthLong
            var received = 0L
            conn.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buf = ByteArray(8_192)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        output.write(buf, 0, n)
                        received += n
                        if (total > 0) onProgress(received.toFloat() / total)
                    }
                }
            }
        }

    fun install(context: Context) {
        val file = apkFile(context)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
