package com.ledger.app.utils

interface HashProvider {
    val algorithm: String
    fun hash(data: ByteArray): ByteArray
}