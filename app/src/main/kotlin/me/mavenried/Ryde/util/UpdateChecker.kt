package me.mavenried.Ryde.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.mavenried.Ryde.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val apkUrl: String,
    val releaseNotes: String,
)

object UpdateChecker {
    private const val GITHUB_OWNER = "mavenried"
    private const val GITHUB_REPO = "Ryde"

    // Returns Result.success(UpdateInfo) if newer, Result.success(null) if up-to-date,
    // Result.failure if network/parse error.
    suspend fun checkForUpdate(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            val code = conn.responseCode
            check(code == 200) { "HTTP $code" }

            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag = json.getString("tag_name").trimStart('v')
            val notes = json.optString("body", "").trim()
            val assets = json.getJSONArray("assets")
            val apkUrl = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith(".apk") }
                ?.getString("browser_download_url")

            if (apkUrl != null && isNewer(tag, BuildConfig.VERSION_NAME))
                UpdateInfo(tag, apkUrl, notes)
            else
                null
        }
    }

    private fun isNewer(remote: String, current: String): Boolean {
        fun parts(v: String) = v.split(".").mapNotNull { it.toIntOrNull() }
        val r = parts(remote)
        val c = parts(current)
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }
}
