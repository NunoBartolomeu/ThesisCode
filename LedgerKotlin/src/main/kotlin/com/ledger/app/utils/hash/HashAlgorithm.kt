package com.ledger.app.utils.hash

interface HashAlgorithm {
    val name: String
    fun hash(data: ByteArray): ByteArray
}