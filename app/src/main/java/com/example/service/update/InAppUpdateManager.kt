package com.example.service.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val releaseNotes: String,
    val downloadUrl: String?
)

object InAppUpdateManager {

    private const val GITHUB_REPO = "SithpongRin/JUMPHUB"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    /**
     * Checks GitHub Releases API asynchronously for a new version.
     */
    suspend fun checkForUpdate(): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val body = json.optString("body", "")

                // Find APK asset download url
                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = if (asset.has("browser_download_url")) asset.getString("browser_download_url") else null
                            break
                        }
                    }
                }

                val currentVersion = BuildConfig.VERSION_NAME
                val isNewer = isVersionNewer(remote = tagName, current = currentVersion)

                return@withContext AppUpdateInfo(
                    hasUpdate = isNewer && !apkUrl.isNullOrEmpty(),
                    latestVersionName = tagName,
                    releaseNotes = body,
                    downloadUrl = apkUrl
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext AppUpdateInfo(
            hasUpdate = false,
            latestVersionName = BuildConfig.VERSION_NAME,
            releaseNotes = "",
            downloadUrl = null
        )
    }

    /**
     * Downloads the APK file in the background via Android DownloadManager
     * and prompts the system package installer immediately upon completion.
     */
    fun startDownloadAndInstall(context: Context, downloadUrl: String, versionName: String) {
        try {
            val fileName = "JUMPHUB-$versionName.apk"

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("JUMPHUB Update $versionName")
                setDescription("Downloading latest update...")
                setMimeType("application/vnd.android.package-archive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            Toast.makeText(context, "កំពុងទាញយកកំណែថ្មី...", Toast.LENGTH_SHORT).show()

            // Register broadcast receiver for download completion
            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        try {
                            recvContext.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        val downloadedFile = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            fileName
                        )
                        installApk(recvContext, downloadedFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Update download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Prompts the native Android package installer to apply the update without uninstalling.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        if (remote.isBlank()) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
