package com.ledger.app.utils

interface HashProvider {
    val algorithm: String
    fun toHashString(byteArray: ByteArray): String = byteArray.joinToString("") { "%02x".format(it) }
    fun toHashByteArray(string: String) = string.toByteArray()
    fun hash(data: String): ByteArray
}