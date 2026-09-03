package com.example.erp.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val versionCode: Long,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: String
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val GITHUB_API = "https://api.github.com/repos/YourNelvi/Dolita/releases/latest"

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion(context)
            Log.d(TAG, "Current version: $currentVersion")

            val connection = URL(GITHUB_API).openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().readText()
            val json = org.json.JSONObject(response)

            val tagName = json.getString("tag_name").removePrefix("v")
            val latestVersionCode = parseVersionCode(tagName)
            val currentVersionCode = getCurrentVersionCode(context)

            Log.d(TAG, "Latest version: $tagName ($latestVersionCode), Current: $currentVersionCode")

            if (latestVersionCode > currentVersionCode) {
                // Find APK asset
                val assets = json.getJSONArray("assets")
                var apkUrl: String? = null

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                if (apkUrl != null) {
                    UpdateInfo(
                        versionName = tagName,
                        versionCode = latestVersionCode,
                        downloadUrl = apkUrl,
                        releaseNotes = json.optString("body", "Sin notas de versión"),
                        publishedAt = json.optString("published_at", "")
                    )
                } else {
                    Log.w(TAG, "No APK found in release")
                    null
                }
            } else {
                Log.d(TAG, "App is up to date")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}")
            null
        }
    }

    private fun getCurrentVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
    }

    private fun getCurrentVersionCode(context: Context): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    private fun parseVersionCode(version: String): Long {
        // Parse "1.12.2" to 1012002
        val parts = version.split(".")
        var code = 0L
        parts.forEachIndexed { index, part ->
            val num = part.toLongOrNull() ?: 0
            when (index) {
                0 -> code += num * 1_000_000
                1 -> code += num * 1_000
                2 -> code += num
            }
        }
        return code
    }
}
