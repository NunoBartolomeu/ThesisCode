package com.ledger.app.utils.hash

import org.reflections.Reflections

object HashProvider {
    private val algorithms: MutableMap<String, HashAlgorithm> = mutableMapOf()

    init {
        val reflections = Reflections("com.ledger.app.utils.hash.implementations")
        reflections.getTypesAnnotatedWith(RegisterOnHashProvider::class.java).forEach { clazz ->
            val instance = clazz.getDeclaredConstructor().newInstance() as HashAlgorithm
            algorithms[instance.name] = instance
            println("Registered Hash Algorithm: ${instance.name}")
        }
        require(algorithms.isNotEmpty()) { "No HashAlgorithm implementations registered." }
    }

    fun toHexString(byteArray: ByteArray): String =
        byteArray.joinToString("") { "%02x".format(it) }

    fun toHashByteArray(string: String): ByteArray =
        string.toByteArray()

    fun hash(data: ByteArray, algorithm: String): ByteArray {
        val hashAlgorithm = algorithms[algorithm]
            ?: throw IllegalArgumentException("Unsupported hash algorithm: $algorithm")
        return hashAlgorithm.hash(data)
    }

    fun hash(data: String, algorithm: String): ByteArray {
        return hash(data.toByteArray(), algorithm)
    }

    fun isSupported(algorithmName: String): Boolean =
        algorithms.containsKey(algorithmName)

    fun getSupportedAlgorithms(): Set<String> =
        algorithms.keys.toSet()

    fun getDefaultAlgorithm(): String =
        algorithms.keys.first()
}