package com.ledger.app.utils.encryption

import org.reflections.Reflections
import javax.crypto.SecretKey

object EncryptionProvider {
    private val algorithms: MutableMap<String, EncryptionAlgorithm> = mutableMapOf()

    init {
        val reflections = Reflections("com.ledger.app.utils.encryption.implementations")
        reflections.getTypesAnnotatedWith(RegisterOnEncryptionProvider::class.java).forEach { clazz ->
            val instance = clazz.getDeclaredConstructor().newInstance() as EncryptionAlgorithm
            algorithms[instance.name] = instance
            println("Registered Encryption Algorithm: ${instance.name}")
        }
        require(algorithms.isNotEmpty()) { "No EncryptionAlgorithm implementations registered." }
    }

    fun toHexString(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    fun hexToByteArray(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun resolve(name: String?): EncryptionAlgorithm =
        algorithms[name ?: algorithms.keys.first()]
            ?: throw IllegalArgumentException("Unsupported encryption algorithm: $name")

    fun encrypt(data: String, algorithm: String? = null): EncryptedPayload =
        resolve(algorithm).encrypt(data.toByteArray())

    fun decrypt(payload: EncryptedPayload, secretKey: SecretKey, algorithm: String? = null): ByteArray =
        resolve(algorithm).decrypt(payload, secretKey)

    fun decrypt(payload: EncryptedPayload, encodedSecretKey: ByteArray, algorithm: String? = null): ByteArray =
        decrypt(payload, resolve(algorithm).byteArrayToSecretKey(encodedSecretKey))

    fun decrypt(payload: EncryptedPayload, hexSecreteKey: String, algorithm: String? = null): ByteArray =
        decrypt(payload, hexToByteArray(hexSecreteKey), algorithm)

    fun isSupported(name: String): Boolean = algorithms.containsKey(name)
    fun getSupportedAlgorithms(): Set<String> = algorithms.keys.toSet()
    fun getDefaultAlgorithm(): String = algorithms.keys.first()
}