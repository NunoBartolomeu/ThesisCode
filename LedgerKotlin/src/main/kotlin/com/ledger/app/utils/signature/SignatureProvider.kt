package com.ledger.app.utils.signature

import org.reflections.Reflections
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

object SignatureProvider {
    private val algorithms: MutableMap<String, SignatureAlgorithm> = mutableMapOf()

    init {
        val reflections = Reflections("com.ledger.app.utils.signature.implementations")
        reflections.getTypesAnnotatedWith(RegisterOnSignatureProvider::class.java).forEach { clazz ->
            val instance = clazz.getDeclaredConstructor().newInstance() as SignatureAlgorithm
            algorithms[instance.name] = instance
            println("Registered Signature Algorithm: ${instance.name}")
        }
        require(algorithms.isNotEmpty()) { "No SignatureAlgorithm implementations registered." }
    }

    fun toHexString(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    fun hexToByteArray(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun resolve(name: String?): SignatureAlgorithm =
        algorithms[name ?: algorithms.keys.first()]
            ?: throw IllegalArgumentException("Unsupported signature algorithm: $name")

    fun sign(data: ByteArray, privateKey: PrivateKey, algorithm: String?): ByteArray =
        resolve(algorithm).sign(data, privateKey)

    fun sign(data: String, hexPrivateKey: String, algorithm: String?): ByteArray =
        sign(data.toByteArray(), bytesToPrivateKey(hexToByteArray(hexPrivateKey), algorithm), algorithm)

    fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey, algorithm: String?): Boolean =
        resolve(algorithm).verify(data, signature, publicKey)

    fun verify(data: String, hexSignature: String, hexPublicKey: String, algorithm: String?): Boolean =
        verify(data.toByteArray(), hexToByteArray(hexSignature), resolve(algorithm).bytesToPublicKey(hexToByteArray(hexPublicKey)), algorithm)

    fun generateKeyPair(algorithm: String?): KeyPair =
        resolve(algorithm).generateKeyPair()
    fun bytesToPublicKey(encodedPublicKey: ByteArray, algorithm: String?): PublicKey =
        resolve(algorithm).bytesToPublicKey(encodedPublicKey)

    fun bytesToPrivateKey(encodedPrivateKey: ByteArray, algorithm: String?): PrivateKey =
        resolve(algorithm).bytesToPrivateKey(encodedPrivateKey)

    fun isSupported(name: String): Boolean = algorithms.containsKey(name)
    fun getSupportedAlgorithms(): Set<String> = algorithms.keys.toSet()
    fun getDefaultAlgorithm(): String = algorithms.keys.first()
}
