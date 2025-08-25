package com.ledger.app.utils.encryption

import javax.crypto.SecretKey

data class EncryptedPayload(
    val encryptedKey: ByteArray,
    val iv: ByteArray,
    val cipherText: ByteArray
)

interface EncryptionAlgorithm {
    val name: String

    fun encrypt(data: ByteArray): EncryptedPayload
    fun decrypt(encryptedPayload: EncryptedPayload, key: SecretKey): ByteArray

    fun byteArrayToSecretKey(keyBytes: ByteArray): SecretKey
    fun stringToSecretKey(hexKey: String): SecretKey

    fun secretKeyToByteArray(secretKey: SecretKey): ByteArray
    fun secretKeyToString(secretKey: SecretKey): String
}