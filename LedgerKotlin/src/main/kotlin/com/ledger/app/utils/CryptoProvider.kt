package com.ledger.app.utils

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

interface CryptoProvider {
    val algorithm: String
    fun encrypt(data: ByteArray, publicKey: PublicKey): EncryptedPayload
    fun decrypt(encryptedPayload: EncryptedPayload, privateKey: PrivateKey): ByteArray
    fun sign(data: ByteArray, privateKey: PrivateKey): ByteArray
    fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean

    fun sign(data: ByteArray, encodedPrivateKey: ByteArray): ByteArray {
        val privateKey = bytesToPrivateKey(encodedPrivateKey)
        return sign(data, privateKey)
    }

    fun verify(data: ByteArray, signature: ByteArray, encodedPublicKey: ByteArray): Boolean {
        val publicKey = bytesToPublicKey(encodedPublicKey)
        return verify(data, signature, publicKey)
    }

    fun generateKeyPair(): KeyPair
    fun bytesToPublicKey(data: ByteArray): PublicKey
    fun bytesToPrivateKey(data: ByteArray): PrivateKey
}

data class EncryptedPayload(
    val encryptedKey: ByteArray,
    val iv: ByteArray,
    val cipherText: ByteArray
)
