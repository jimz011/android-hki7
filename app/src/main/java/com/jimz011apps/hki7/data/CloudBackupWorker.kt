package com.jimz011apps.hki7.data

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

fun driveAuthorizationRequest(): AuthorizationRequest = AuthorizationRequest.builder()
    .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
    .build()

private suspend fun driveAccessToken(context: Context): String = withContext(Dispatchers.IO) {
    val result = Tasks.await(Identity.getAuthorizationClient(context).authorize(driveAuthorizationRequest()))
    check(!result.hasResolution()) { "Google Drive authorization requires user interaction" }
    result.accessToken ?: error("Google Drive did not return an access token")
}

data class CloudBackupFile(val id: String, val name: String, val modifiedTime: String)

class CloudBackupWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)
        if (!prefs.cloudBackupEnabled.first()) return Result.success()
        return runCatching {
            CloudBackupStorage.write(applicationContext, prefs.exportUiBackup())
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }
}

object CloudBackupStorage {
    private const val API = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD_API = "https://www.googleapis.com/upload/drive/v3/files"
    private const val MAX_BACKUPS = 14
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun backups(context: Context): List<CloudBackupFile> = withContext(Dispatchers.IO) {
        val token = driveAccessToken(context)
        val query = "name contains 'hki7-backup-' and trashed = false"
        val url = "$API?spaces=appDataFolder&q=${encode(query)}&orderBy=modifiedTime%20desc&fields=files(id,name,modifiedTime)"
        val root = json.parseToJsonElement(request(url, token)).jsonObject
        root["files"]?.jsonArray.orEmpty().mapNotNull { element ->
            val item = element.jsonObject
            val id = item["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            CloudBackupFile(
                id = id,
                name = item["name"]?.jsonPrimitive?.content ?: "Cloud backup",
                modifiedTime = item["modifiedTime"]?.jsonPrimitive?.content.orEmpty()
            )
        }
    }

    suspend fun write(context: Context, raw: String) = withContext(Dispatchers.IO) {
        val token = driveAccessToken(context)
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val name = "hki7-backup-$stamp.json"
        val boundary = "hki7-${System.currentTimeMillis()}"
        val metadata = "{\"name\":\"$name\",\"parents\":[\"appDataFolder\"]}"
        val body = buildString {
            append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\nContent-Type: application/json\r\n\r\n")
            append(raw)
            append("\r\n--$boundary--\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        request(
            "$UPLOAD_API?uploadType=multipart&fields=id",
            token,
            method = "POST",
            contentType = "multipart/related; boundary=$boundary",
            body = body
        )
        backupsWithToken(token).drop(MAX_BACKUPS).forEach { backup ->
            request("$API/${encode(backup.id)}", token, method = "DELETE")
        }
    }

    suspend fun read(context: Context, fileId: String): String = withContext(Dispatchers.IO) {
        request("$API/${encode(fileId)}?alt=media", driveAccessToken(context))
    }

    private fun backupsWithToken(token: String): List<CloudBackupFile> {
        val query = "name contains 'hki7-backup-' and trashed = false"
        val url = "$API?spaces=appDataFolder&q=${encode(query)}&orderBy=modifiedTime%20desc&fields=files(id,name,modifiedTime)"
        val root = json.parseToJsonElement(request(url, token)).jsonObject
        return root["files"]?.jsonArray.orEmpty().mapNotNull { element ->
            val item = element.jsonObject
            val id = item["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            CloudBackupFile(id, item["name"]?.jsonPrimitive?.content ?: "Cloud backup", item["modifiedTime"]?.jsonPrimitive?.content.orEmpty())
        }
    }

    private fun request(
        url: String,
        token: String,
        method: String = "GET",
        contentType: String? = null,
        body: ByteArray? = null
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (contentType != null) connection.setRequestProperty("Content-Type", contentType)
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(code in 200..299) { "Google Drive request failed ($code): $response" }
            response
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}

object CloudBackupWork {
    private const val PERIODIC_NAME = "hki7_cloud_backup_daily"
    private const val ONESHOT_NAME = "hki7_cloud_backup_now"

    private fun constraints() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedule(context: Context) {
        val periodic = PeriodicWorkRequestBuilder<CloudBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints()).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic
        )
        backupNow(context)
    }

    fun backupNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<CloudBackupWorker>().setConstraints(constraints()).build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONESHOT_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(ONESHOT_NAME)
    }
}
