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

    fun keyOrSigToString(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    fun keyOrSigToByteArray(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    fun dataToString(data: ByteArray): String = String(data)
    fun dataToByteArray(data: String): ByteArray = data.toByteArray()

    private fun resolve(name: String?): SignatureAlgorithm =
        algorithms[name ?: algorithms.keys.first()]
            ?: throw IllegalArgumentException("Unsupported signature algorithm: $name")

    fun sign(data: String, privateKey: PrivateKey, algorithm: String?): ByteArray =
        resolve(algorithm).sign(dataToByteArray(data), privateKey)

    fun sign(data: String, encodedPrivateKey: ByteArray, algorithm: String?): ByteArray =
        resolve(algorithm).let { it.sign(dataToByteArray(data), it.bytesToPrivateKey(encodedPrivateKey)) }

    fun sign(data: String, hexPrivateKey: String, algorithm: String?): ByteArray =
        sign(data, keyOrSigToByteArray(hexPrivateKey), algorithm)

    fun verify(data: String, signature: ByteArray, publicKey: PublicKey, algorithm: String?): Boolean =
        resolve(algorithm).verify(dataToByteArray(data), signature, publicKey)

    fun verify(data: String, signature: ByteArray, encodedPublicKey: ByteArray, algorithm: String?): Boolean =
        resolve(algorithm).let { it.verify(dataToByteArray(data), signature, it.bytesToPublicKey(encodedPublicKey)) }

    fun verify(data: String, hexSignature: String, hexPublicKey: String, algorithm: String?): Boolean =
        verify(data, keyOrSigToByteArray(hexSignature), keyOrSigToByteArray(hexPublicKey), algorithm)

    fun generateKeyPair(algorithm: String?): KeyPair =
        resolve(algorithm).generateKeyPair()

    fun isSupported(name: String): Boolean = algorithms.containsKey(name)
    fun getSupportedAlgorithms(): Set<String> = algorithms.keys.toSet()
    fun getDefaultAlgorithm(): String = algorithms.keys.first()
}
