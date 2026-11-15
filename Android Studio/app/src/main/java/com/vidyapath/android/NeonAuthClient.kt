package com.vidyapath.android

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AuthUser(val id: String, val name: String, val email: String)

class NeonAuthClient(context: Context, private val baseUrl: String) {
    private val sessions = EncryptedSessionStore(context)
    private val authBase = baseUrl.trimEnd('/')

    suspend fun sendEmailCode(email: String): Unit = withContext(Dispatchers.IO) {
        ensureConfigured()
        request("/email-otp/send-verification-otp", "POST", JSONObject().put("email", email).put("type", "sign-in"))
        Unit
    }

    suspend fun signIn(email: String, code: String, name: String?): AuthUser = withContext(Dispatchers.IO) {
        ensureConfigured()
        val body = JSONObject().put("email", email).put("otp", code)
        name?.takeIf(String::isNotBlank)?.let { body.put("name", it) }
        request("/sign-in/email-otp", "POST", body, persistSession = true)
        val session = request("/get-session", "GET")
        val user = parseUser(session)
        sessions.saveFromResponse(session.headers)
        user ?: throw IllegalStateException("Neon Auth did not return a signed-in user.")
    }

    suspend fun restoreSession(): AuthUser? = withContext(Dispatchers.IO) {
        ensureConfigured()
        if (sessions.cookie().isNullOrBlank()) return@withContext null
        try {
            val response = request("/get-session", "GET")
            parseUser(response)
        } catch (error: AuthHttpException) {
            if (error.statusCode == 401 || error.statusCode == 403) {
                sessions.clear()
                null
            } else {
                throw error
            }
        }
    }

    suspend fun updateName(name: String): Unit = withContext(Dispatchers.IO) {
        request("/update-user", "POST", JSONObject().put("name", name))
        Unit
    }

    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        try {
            if (!sessions.cookie().isNullOrBlank()) request("/sign-out", "POST", JSONObject())
        } finally {
            sessions.clear()
        }
        Unit
    }

    private fun ensureConfigured() {
        val parsed = runCatching { URL(authBase) }.getOrNull()
        require(parsed != null && parsed.protocol == "https" && parsed.host.isNotBlank()) {
            "Set neonAuthBaseUrl in Android Studio/local.properties to enable email sign-in."
        }
    }

    private fun request(
        path: String,
        method: String,
        body: JSONObject? = null,
        persistSession: Boolean = false,
    ): AuthResponse {
        val connection = (URL("$authBase$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            sessions.cookie()?.let { setRequestProperty("Cookie", it) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val headers = connection.headerFields
            if (persistSession) sessions.saveFromResponse(headers)
            if (status !in 200..299) {
                val message = runCatching { JSONObject(responseBody).optString("message").takeIf(String::isNotBlank) }.getOrNull()
                    ?: "Authentication request failed (HTTP $status)."
                throw AuthHttpException(status, message)
            }
            return AuthResponse(responseBody, headers)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseUser(response: AuthResponse): AuthUser? {
        val json = runCatching { JSONObject(response.body) }.getOrNull() ?: return null
        val user = json.optJSONObject("user")
            ?: json.optJSONObject("data")?.optJSONObject("user")
            ?: return null
        val id = user.optString("id").takeIf(String::isNotBlank) ?: return null
        val email = user.optString("email").takeIf(String::isNotBlank) ?: return null
        val name = user.optString("name").takeIf(String::isNotBlank) ?: email.substringBefore('@')
        return AuthUser(id, name, email)
    }

    private data class AuthResponse(val body: String, val headers: Map<String?, List<String>?>)
    private class AuthHttpException(val statusCode: Int, message: String) : IllegalStateException(message)
}

private class EncryptedSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("vidyapath_auth", Context.MODE_PRIVATE)

    fun cookie(): String? {
        val encoded = preferences.getString(KEY_COOKIE, null) ?: return null
        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, IV_LENGTH)
            val cipherText = payload.copyOfRange(IV_LENGTH, payload.size)
            Cipher.getInstance(TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
                String(doFinal(cipherText), Charsets.UTF_8)
            }
        }.getOrElse {
            clear()
            null
        }
    }

    fun saveFromResponse(headers: Map<String?, List<String>?>) {
        val cookies = headers.entries
            .filter { it.key.equals("Set-Cookie", ignoreCase = true) }
            .flatMap { it.value.orEmpty() }
            .map { it.substringBefore(';').trim() }
            .filter { it.substringBefore('=').contains("session", ignoreCase = true) && '=' in it }
        if (cookies.isEmpty()) return
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.iv + cipher.doFinal(cookies.joinToString("; ").toByteArray(Charsets.UTF_8))
        preferences.edit().putString(KEY_COOKIE, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_COOKIE).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val KEY_ALIAS = "vidyapath.auth.session.v1"
        const val KEY_COOKIE = "encrypted_cookie"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
    }
}
