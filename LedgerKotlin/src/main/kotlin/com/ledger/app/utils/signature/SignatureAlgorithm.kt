package com.ledger.app.utils.signature

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

interface SignatureAlgorithm {
    val name: String
    fun sign(data: ByteArray, privateKey: PrivateKey): ByteArray
    fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean
    fun generateKeyPair(): KeyPair
    fun bytesToPublicKey(encodedPublicKey: ByteArray): PublicKey
    fun bytesToPrivateKey(encodedPrivateKey: ByteArray): PrivateKey
}
