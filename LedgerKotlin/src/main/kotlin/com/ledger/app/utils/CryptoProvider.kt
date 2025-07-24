package com.ledger.app.utils

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

interface CryptoProvider {
    val algorithm: String

    fun keyOrSigToString(keyOrSig: ByteArray): String = keyOrSig.joinToString("") { "%02x".format(it) }
    fun dataToString(data: ByteArray): String = String(data)
    fun keyOrSigToByteArray(keyOrSig: String): ByteArray = keyOrSig.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    fun dataToByteArray(data: String): ByteArray = data.toByteArray()

    fun encrypt(data: String, publicKey: PublicKey): EncryptedPayload
    fun decrypt(encryptedPayload: EncryptedPayload, privateKey: PrivateKey): ByteArray

    fun sign(data: String, privateKey: PrivateKey): ByteArray
    fun sign(data: String, encodedPrivateKey: ByteArray): ByteArray =
        sign(data, bytesToPrivateKey(encodedPrivateKey))
    fun sign(data: String, encodedPrivateKey: String): ByteArray =
        sign(data, keyOrSigToByteArray(encodedPrivateKey))



    fun verify(data: String, signature: ByteArray, publicKey: PublicKey): Boolean
    fun verify(data: String, signature: ByteArray, encodedPublicKey: ByteArray): Boolean =
        verify(data, signature, bytesToPublicKey(encodedPublicKey))
    fun verify(data: String, signature: String, encodedPublicKey: String): Boolean =
        verify(data, keyOrSigToByteArray(signature), keyOrSigToByteArray(encodedPublicKey))

    fun generateKeyPair(): KeyPair
    fun bytesToPublicKey(encodedPublicKey: ByteArray): PublicKey
    fun bytesToPrivateKey(encodedPrivateKey: ByteArray): PrivateKey
}

data class EncryptedPayload(
    val encryptedKey: ByteArray,
    val iv: ByteArray,
    val cipherText: ByteArray
)
