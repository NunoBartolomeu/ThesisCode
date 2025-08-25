package com.ledger.app.utils.encryption.implementations

import com.ledger.app.utils.encryption.EncryptedPayload
import com.ledger.app.utils.encryption.EncryptionAlgorithm
import com.ledger.app.utils.encryption.RegisterOnEncryptionProvider
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@RegisterOnEncryptionProvider
class AESEncryptionAlgorithm : EncryptionAlgorithm {
    override val name: String = "AES"

    companion object {
        private const val AES_ALGORITHM = "AES"
        private const val AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val AES_KEY_SIZE = 256 // bits
        private const val AES_IV_SIZE = 16   // bytes
    }

    override fun encrypt(data: ByteArray): EncryptedPayload {
        val key = KeyGenerator.getInstance(AES_ALGORITHM).apply {
            init(AES_KEY_SIZE)
        }.generateKey()

        val iv = ByteArray(AES_IV_SIZE).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        }
        val cipherText = cipher.doFinal(data)

        return EncryptedPayload(key.encoded, iv, cipherText)
    }

    override fun decrypt(encryptedPayload: EncryptedPayload, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, IvParameterSpec(encryptedPayload.iv))
        }

        return cipher.doFinal(encryptedPayload.cipherText)
    }

    override fun byteArrayToSecretKey(keyBytes: ByteArray): SecretKey {
        require(keyBytes.size == 32) {
            "AES-256 key must be exactly 32 bytes, got ${keyBytes.size} bytes"
        }
        return SecretKeySpec(keyBytes, AES_ALGORITHM)
    }

    override fun stringToSecretKey(hexKey: String): SecretKey {
        require(hexKey.matches(Regex("^[0-9a-fA-F]+$"))) {
            "Invalid hex string format"
        }
        require(hexKey.length == 64) {
            "AES-256 hex key must be exactly 64 characters (32 bytes), got ${hexKey.length} characters"
        }

        val keyBytes = hexKey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return byteArrayToSecretKey(keyBytes)
    }

    override fun secretKeyToByteArray(secretKey: SecretKey): ByteArray {
        require(secretKey.algorithm == AES_ALGORITHM) {
            "Expected AES SecretKey, got ${secretKey.algorithm}"
        }
        return secretKey.encoded
    }

    override fun secretKeyToString(secretKey: SecretKey): String {
        val keyBytes = secretKeyToByteArray(secretKey)
        return keyBytes.joinToString("") { "%02x".format(it) }
    }
}