package com.olius.app.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Refresh token da Core salvo no aparelho + até quando ele vale. */
data class StoredRefreshToken(val token: String, val expiresAtMillis: Long?) {
    override fun toString() = "StoredRefreshToken[redacted]"
}

/** Onde o refresh token fica entre uma abertura do app e outra. */
interface RefreshTokenStore {
    fun load(): StoredRefreshToken?
    fun save(token: StoredRefreshToken)
    fun clear()
}

class InMemoryRefreshTokenStore : RefreshTokenStore {
    private var token: StoredRefreshToken? = null
    override fun load() = token
    override fun save(token: StoredRefreshToken) { this.token = token }
    override fun clear() { token = null }
}

/** Cifra e decifra textos curtos. */
interface SecretCipher {
    fun encrypt(plain: String): String
    /** null se não der para decifrar (ex.: chave apagada ou backup restaurado em outro aparelho). */
    fun decrypt(encoded: String): String?
}

/** AES-256/GCM com a chave gerada e guardada no Android Keystore. */
class KeystoreSecretCipher(private val alias: String = KEY_ALIAS) : SecretCipher {
    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    override fun decrypt(encoded: String): String? = runCatching {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }.getOrNull()

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        // Sem setUserAuthenticationRequired: o refresh token é lido ao abrir o app para renovar a sessão
        // sozinho, sem pedir biometria/PIN. Exigir autenticação quebraria o login automático, e aparelhos
        // sem tela de bloqueio nem conseguiriam gerar a chave. A chave não sai do Keystore do aparelho.
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT) // NOSONAR kotlin:S6288
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "olius_refresh_token_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}

/** Refresh token cifrado ([cipher]) em SharedPreferences próprias. */
class EncryptedRefreshTokenStore(
    context: Context,
    private val cipher: SecretCipher = KeystoreSecretCipher()
) : RefreshTokenStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): StoredRefreshToken? {
        val encrypted = prefs.getString(KEY_TOKEN, null) ?: return null
        val token = cipher.decrypt(encrypted) ?: return null.also { clear() }
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, NO_EXPIRY).takeIf { it != NO_EXPIRY }
        return StoredRefreshToken(token, expiresAt)
    }

    override fun save(token: StoredRefreshToken) {
        prefs.edit()
            .putString(KEY_TOKEN, cipher.encrypt(token.token))
            .putLong(KEY_EXPIRES_AT, token.expiresAtMillis ?: NO_EXPIRY)
            .commit()
    }

    override fun clear() {
        prefs.edit().clear().commit()
    }

    private companion object {
        const val PREFS_NAME = "olius_auth"
        const val KEY_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "refresh_expires_at"
        const val NO_EXPIRY = -1L
    }
}
