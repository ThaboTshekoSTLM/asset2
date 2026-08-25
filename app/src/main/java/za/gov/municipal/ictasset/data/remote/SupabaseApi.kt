package za.gov.municipal.ictasset.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SupabaseApi(
    context: Context,
    private val projectUrl: String,
    private val publishableKey: String
) {
    private val applicationContext = context.applicationContext
    private val preferences = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = preferences.getString("access_token", null)
        private set(value) = preferences.edit().putString("access_token", value).apply()

    var userId: String?
        get() = preferences.getString("user_id", null)
        private set(value) = preferences.edit().putString("user_id", value).apply()

    val configured: Boolean
        get() = projectUrl.startsWith("https://") && publishableKey.isNotBlank()

    suspend fun login(email: String, password: String): JSONObject {
        val response = request(
            method = "POST",
            path = "/auth/v1/token?grant_type=password",
            body = JSONObject().put("email", email).put("password", password),
            authenticated = false
        ) as JSONObject
        accessToken = response.getString("access_token")
        userId = response.getJSONObject("user").getString("id")
        return fetchProfile(userId!!)
    }

    suspend fun fetchProfile(id: String): JSONObject {
        val rows = request("GET", "/rest/v1/profiles?id=eq.$id&select=*", authenticated = true) as JSONArray
        if (rows.length() == 0) error("No active app profile was found for this account.")
        return rows.getJSONObject(0)
    }

    suspend fun fetchProfiles(): JSONArray =
        request("GET", "/rest/v1/profiles?select=*&order=full_name.asc", authenticated = true) as JSONArray

    suspend fun fetchAssets(): JSONArray =
        request("GET", "/rest/v1/assets?select=*&order=registered_at.desc", authenticated = true) as JSONArray

    suspend fun fetchMovements(): JSONArray =
        request("GET", "/rest/v1/asset_movements?select=*&order=movement_date.desc", authenticated = true) as JSONArray

    suspend fun insertAsset(payload: JSONObject): JSONObject {
        val rows = request("POST", "/rest/v1/assets", payload, authenticated = true, preferRepresentation = true) as JSONArray
        return rows.getJSONObject(0)
    }

    suspend fun insertMovement(payload: JSONObject) {
        request("POST", "/rest/v1/asset_movements", payload, authenticated = true)
    }

    suspend fun uploadCompressedAssetPhoto(localPath: String, assetId: String): String =
        withContext(Dispatchers.IO) {
            val source = BitmapFactory.decodeFile(localPath)
                ?: error("The captured asset photo could not be read.")
            val longestSide = maxOf(source.width, source.height)
            val scaled = if (longestSide > 960) {
                val scale = 960f / longestSide
                Bitmap.createScaledBitmap(
                    source,
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                source
            }
            val bytes = ByteArrayOutputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 48, output)
                output.toByteArray()
            }
            if (scaled !== source) scaled.recycle()
            source.recycle()

            val objectPath = "${userId ?: error("Please sign in again.")}/$assetId.jpg"
            storageRequest("POST", objectPath, bytes)
            objectPath
        }

    suspend fun downloadAssetPhotoToCache(objectPath: String): String? =
        withContext(Dispatchers.IO) {
            if (objectPath.isBlank()) return@withContext null
            val cacheDirectory = File(applicationContext.cacheDir, "asset_photos").also { it.mkdirs() }
            val cacheFile = File(cacheDirectory, "${objectPath.hashCode().toUInt()}.jpg")
            if (!cacheFile.exists()) {
                val bytes = storageRequest("GET", objectPath)
                cacheFile.writeBytes(bytes)
            }
            cacheFile.absolutePath
        }

    suspend fun recordMovement(payload: JSONObject) {
        request("POST", "/rest/v1/rpc/record_asset_movement", payload, authenticated = true)
    }

    fun signOut() {
        preferences.edit().clear().apply()
    }

    private fun storageRequest(method: String, objectPath: String, body: ByteArray? = null): ByteArray {
        check(configured) { "Supabase is not configured in local.properties." }
        val connection = URL(projectUrl.trimEnd('/') + "/storage/v1/object/asset-photos/$objectPath")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("apikey", publishableKey)
            connection.setRequestProperty(
                "Authorization",
                "Bearer ${accessToken ?: error("Please sign in again.")}"
            )
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "image/jpeg")
                connection.setRequestProperty("x-upsert", "false")
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            val bytes = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.use { it.readBytes() }
                ?: ByteArray(0)
            if (status !in 200..299) {
                val message = runCatching { JSONObject(String(bytes)).optString("message") }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: "Asset photo request failed ($status)."
                error(message)
            }
            return bytes
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean,
        preferRepresentation: Boolean = false
    ): Any = withContext(Dispatchers.IO) {
        check(configured) { "Supabase is not configured in local.properties." }
        val connection = URL(projectUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("apikey", publishableKey)
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            if (authenticated) {
                val token = accessToken ?: error("Please sign in again.")
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (preferRepresentation) connection.setRequestProperty("Prefer", "return=representation")
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: "Supabase request failed ($status)."
                error(message)
            }
            if (text.isBlank()) JSONObject() else if (text.trimStart().startsWith("[")) JSONArray(text) else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
