package com.vidyapath.android

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class LocalUser(val id: String, val name: String, val email: String)

class LocalAccountStore(context: Context) {
    private val preferences = context.getSharedPreferences("vidyapath_local_accounts", Context.MODE_PRIVATE)

    suspend fun createAccount(name: String, email: String, password: String, confirmation: String): LocalUser =
        withContext(Dispatchers.IO) {
            val normalizedName = name.trim()
            require(normalizedName.isNotBlank()) { "Enter your full name." }
            val normalizedEmail = normalizeAccountEmail(email)
            require(password.length >= MIN_PASSWORD_LENGTH) { "Password must be at least 8 characters." }
            require(password == confirmation) { "Passwords do not match." }

            val key = accountKey(normalizedEmail)
            require(!preferences.contains(key)) { "An account with this email already exists. Sign in instead." }

            val credential = PasswordHasher.create(password)
            val account = JSONObject()
                .put("name", normalizedName)
                .put("salt", Base64.getEncoder().encodeToString(credential.salt))
                .put("passwordHash", Base64.getEncoder().encodeToString(credential.hash))
            check(
                preferences.edit()
                    .putString(key, account.toString())
                    .putString(KEY_ACTIVE_EMAIL, normalizedEmail)
                    .commit(),
            ) { "Unable to save this account on the device." }
            LocalUser(normalizedEmail, normalizedName, normalizedEmail)
        }

    suspend fun signIn(email: String, password: String): LocalUser = withContext(Dispatchers.IO) {
        val normalizedEmail = normalizeAccountEmail(email)
        require(password.isNotEmpty()) { "Enter your password." }
        val account = preferences.getString(accountKey(normalizedEmail), null)
            ?: throw IllegalArgumentException("No account exists for this email. Create an account first.")
        val record = JSONObject(account)
        val salt = decodeCredential(record.getString("salt"))
        val expectedHash = decodeCredential(record.getString("passwordHash"))
        require(PasswordHasher.matches(password, salt, expectedHash)) { "Email or password is incorrect." }
        val name = record.getString("name")
        check(preferences.edit().putString(KEY_ACTIVE_EMAIL, normalizedEmail).commit()) {
            "Unable to save the local sign-in."
        }
        LocalUser(normalizedEmail, name, normalizedEmail)
    }

    suspend fun restoreSession(): LocalUser? = withContext(Dispatchers.IO) {
        val email = preferences.getString(KEY_ACTIVE_EMAIL, null) ?: return@withContext null
        val record = preferences.getString(accountKey(email), null) ?: run {
            signOutOnIo()
            return@withContext null
        }
        val account = JSONObject(record)
        LocalUser(email, account.getString("name"), email)
    }

    suspend fun updateName(email: String, name: String) = withContext(Dispatchers.IO) {
        val normalizedEmail = normalizeAccountEmail(email)
        val normalizedName = name.trim()
        require(normalizedName.isNotBlank()) { "Enter your full name to continue." }
        val key = accountKey(normalizedEmail)
        val record = preferences.getString(key, null)
            ?: throw IllegalStateException("The local account could not be found.")
        val updated = JSONObject(record).put("name", normalizedName)
        check(preferences.edit().putString(key, updated.toString()).commit()) {
            "Unable to update the local account."
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        signOutOnIo()
    }

    private fun signOutOnIo() {
        check(preferences.edit().remove(KEY_ACTIVE_EMAIL).commit()) {
            "Unable to sign out of the local account."
        }
    }

    private fun accountKey(email: String): String =
        "account:${Base64.getEncoder().encodeToString(email.toByteArray(Charsets.UTF_8))}"

    private fun decodeCredential(value: String): ByteArray = try {
        Base64.getDecoder().decode(value)
    } catch (error: IllegalArgumentException) {
        throw IllegalStateException("The saved local account is invalid.", error)
    }

    private companion object {
        const val KEY_ACTIVE_EMAIL = "active_email"
        const val MIN_PASSWORD_LENGTH = 8
    }
}

internal fun normalizeAccountEmail(email: String): String {
    val normalized = email.trim().lowercase(Locale.ROOT)
    require(normalized.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) { "Enter a valid email address." }
    return normalized
}

internal data class PasswordCredential(val salt: ByteArray, val hash: ByteArray)

internal object PasswordHasher {
    private const val ITERATIONS = 600_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private val secureRandom = SecureRandom()

    fun create(password: String): PasswordCredential {
        val salt = ByteArray(SALT_LENGTH_BYTES).also(secureRandom::nextBytes)
        return PasswordCredential(salt, derive(password, salt))
    }

    fun matches(password: String, salt: ByteArray, expectedHash: ByteArray): Boolean =
        MessageDigest.isEqual(derive(password, salt), expectedHash)

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }
}
