package com.ledger.app.models.ledger

data class Receipt(
    val entry: Entry,
    val timestamp: Long,
    val requesterId: String,
    val proof: List<String>,
    val hash: String,
    val signatureData: String,
    val publicKey: String,
    val algorithm: String
)
