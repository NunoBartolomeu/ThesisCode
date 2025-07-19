package com.ledger.app.models.ledger

import com.ledger.app.models.ledger.Entry

data class PageSummary(
    val ledgerName: String,
    val number: Int,
    val timestamp: Long,
    val previousHash: ByteArray?,
    val merkleRoot: ByteArray,
    val entryCount: Int,
    val entryIds: List<String>
)

data class Page(
    val ledgerName: String,
    val number: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val previousHash: ByteArray?,
    val merkleRoot: ByteArray,
    val entries: List<Entry>,
) {
    fun toPageSummary() = PageSummary(ledgerName, number, timestamp, previousHash, merkleRoot, entries.count(), entries.map { it.id })
    fun computeHash(hash: (ByteArray) -> ByteArray): ByteArray {
        val data = number.toString().toByteArray() +
                (previousHash ?: ByteArray(0)) +
                merkleRoot +
                timestamp.toString().toByteArray()
        return hash(data)
    }
}