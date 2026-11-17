/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import net.openid.appauth.AuthState
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/**
 * Stan logowania w zaszyfrowanym pliku [file], który nie trafia do kopii zapasowej.
 *
 * Wymagania: US14.4, NFR-35
 */
internal class LoginVault(private val file: File, private val cipher: LoginCipher) {
    /** Zapisany stan logowania albo `null`, gdy go nie ma albo nie da się go odszyfrować. */
    @Synchronized
    fun read(): AuthState? =
        file.takeIf { it.exists() }?.let { saved ->
            runCatching { AuthState.jsonDeserialize(cipher.open(saved.readBytes()).decodeToString()) }.getOrNull()
        }

    /** Zapisuje [login] w miejsce poprzedniego stanu. */
    @Synchronized
    fun write(login: AuthState) {
        file.writeBytes(cipher.seal(login.jsonSerializeString().encodeToByteArray()))
    }

    /** Usuwa zapisany stan. */
    @Synchronized
    fun clear() {
        file.delete()
    }
}

/** Szyfrowanie zapisu logowania. */
internal interface LoginCipher {
    /** Szyfruje [plain]. */
    fun seal(plain: ByteArray): ByteArray

    /** Odszyfrowuje wynik [seal]; błąd, gdy dane są uszkodzone albo klucza już nie ma. */
    fun open(sealed: ByteArray): ByteArray
}

/** AES-GCM kluczem z Android Keystore, który nie opuszcza magazynu systemu. Wymagania: NFR-35 */
internal class KeystoreCipher
    @Inject
    constructor() : LoginCipher {
        override fun seal(plain: ByteArray): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
            return cipher.iv + cipher.doFinal(plain)
        }

        override fun open(sealed: ByteArray): ByteArray {
            val spec = GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key(), spec) }
            return cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
        }

        private fun key(): SecretKey {
            val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            return store.getKey(ALIAS, null) as? SecretKey ?: newKey()
        }

        private fun newKey(): SecretKey {
            val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val spec =
                KeyGenParameterSpec
                    .Builder(ALIAS, purposes)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
                init(spec)
                generateKey()
            }
        }

        private companion object {
            const val KEYSTORE = "AndroidKeyStore"
            const val ALIAS = "logowanie"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val TAG_BITS = 128
            const val IV_BYTES = 12
        }
    }
