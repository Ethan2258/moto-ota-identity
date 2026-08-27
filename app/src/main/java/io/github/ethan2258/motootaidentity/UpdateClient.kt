package io.github.ethan2258.motootaidentity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class AvailableUpdate(
    val versionName: String,
    val apkUrl: String,
    val checksumUrl: String,
)

object UpdateClient {
    private const val RELEASE_API =
        "https://api.github.com/repos/Ethan2258/moto-ota-identity/releases/latest"
    private val USER_AGENT = "MotoOtaIdentity/${BuildConfig.VERSION_NAME}"
    private const val MAX_APK_BYTES = 50L * 1024L * 1024L

    suspend fun checkLatest(): AvailableUpdate? = withContext(Dispatchers.IO) {
        val release = JSONObject(readText(RELEASE_API, 512 * 1024))
        val version = release.getString("tag_name").removePrefix("v")
        if (!VersionComparator.isNewer(version, BuildConfig.VERSION_NAME)) {
            return@withContext null
        }

        val assets = release.getJSONArray("assets")
        var apkUrl: String? = null
        var checksumUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val name = asset.getString("name")
            val url = asset.getString("browser_download_url")
            require(Uri.parse(url).scheme == "https") { "Release asset must use HTTPS" }
            when {
                name.endsWith(".apk", ignoreCase = true) -> apkUrl = url
                name.endsWith(".sha256", ignoreCase = true) -> checksumUrl = url
            }
        }
        AvailableUpdate(
            versionName = version,
            apkUrl = requireNotNull(apkUrl) { "Release APK is missing" },
            checksumUrl = requireNotNull(checksumUrl) { "Release checksum is missing" },
        )
    }

    suspend fun downloadVerified(
        context: Context,
        update: AvailableUpdate,
        onProgress: suspend (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val expectedHash = readText(update.checksumUrl, 4096)
            .trim()
            .substringBefore(' ')
            .lowercase()
        require(expectedHash.matches(Regex("[0-9a-f]{64}"))) { "Invalid SHA-256 file" }

        val updateDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
        val destination = File(updateDirectory, "MotoOtaIdentity-${update.versionName}.apk")
        val connection = open(update.apkUrl)
        try {
            val contentLength = connection.contentLengthLong
            require(contentLength in 1..MAX_APK_BYTES) { "Unexpected APK size" }
            val digest = MessageDigest.getInstance("SHA-256")
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    var lastProgress = -1
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_APK_BYTES) { "APK exceeds size limit" }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        val progress = ((total * 100) / contentLength).toInt().coerceIn(0, 100)
                        if (progress != lastProgress) {
                            lastProgress = progress
                            onProgress(progress)
                        }
                    }
                }
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            require(actualHash == expectedHash) { "APK SHA-256 mismatch" }
            destination
        } catch (error: Throwable) {
            destination.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun requestInstall(context: Context, apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
            return false
        }
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updates",
            apk,
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
        return true
    }

    private fun readText(url: String, limit: Int): String {
        val connection = open(url)
        return try {
            connection.inputStream.bufferedReader().use { reader ->
                val result = StringBuilder()
                val buffer = CharArray(4096)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    require(result.length + count <= limit) { "Response exceeds size limit" }
                    result.append(buffer, 0, count)
                }
                result.toString()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection {
        val parsed = URL(url)
        require(parsed.protocol == "https") { "Only HTTPS is allowed" }
        return (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", USER_AGENT)
            connect()
            require(responseCode in 200..299) { "HTTP $responseCode" }
        }
    }
}
