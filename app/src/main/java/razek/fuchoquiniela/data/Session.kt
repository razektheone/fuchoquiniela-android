package razek.fuchoquiniela.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Holds the signed-in user and persists the API bearer token, encrypted with a key that lives in
 * the Android Keystore (the iOS app keeps it in the Keychain).
 */
object Session {
    private const val PREFS = "session"
    private const val TOKEN_KEY = "mobile-token"
    private const val KEY_ALIAS = "fuchoquiniela.api"

    private lateinit var prefs: SharedPreferences

    @Volatile
    var token: String? = null
        private set

    private val _user = MutableStateFlow<User?>(null)

    /** The signed-in user; filled by login or by `FuchoApi.me()` after launch. */
    val user: StateFlow<User?> = _user

    private val _signedIn = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = _signedIn

    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when an authenticated request got a 401, after the session was cleared. */
    val expired: SharedFlow<Unit> = _expired

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        token = runCatching { prefs.getString(TOKEN_KEY, null)?.let(::decrypt) }.getOrNull()
        _signedIn.value = token != null
    }

    fun setUser(user: User) {
        _user.value = user
    }

    fun signIn(token: String, user: User) {
        this.token = token
        _user.value = user
        runCatching { prefs.edit().putString(TOKEN_KEY, encrypt(token)).apply() }
        _signedIn.value = true
    }

    fun signOut() {
        token = null
        _user.value = null
        prefs.edit().remove(TOKEN_KEY).apply()
        _signedIn.value = false
    }

    /** Called by the API client on a 401; the app goes back to the login screen. */
    fun expire() {
        if (token == null) return
        signOut()
        _expired.tryEmit(Unit)
    }

    // Keystore

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(text: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val data = cipher.iv + cipher.doFinal(text.toByteArray())
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val data = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data, 0, 12))
        return String(cipher.doFinal(data, 12, data.size - 12))
    }
}
