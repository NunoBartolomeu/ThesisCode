package com.ledger.app.models.ledger

data class Entry(
    val id: String,
    val timestamp: Long,

    val content: ByteArray,
    val senders: List<String>,
    val recipients: List<String>,

    val signatures: MutableList<Signature>,

    val relatedEntries: MutableList<String>,
    val keywords: MutableList<String>,

    var ledgerName: String,
    var pageNum: Int? = null
) {
    data class Signature(val signerId: String, val publicKey: ByteArray, val signature: ByteArray)

    fun getRawData(): ByteArray {
        return id.toByteArray().plus("|".toByteArray())
            .plus(timestamp.toString().toByteArray()).plus("|".toByteArray())
            .plus(content).plus("|".toByteArray())
            .plus(senders.joinToString().toByteArray()).plus("|".toByteArray())
            .plus(recipients.joinToString().toByteArray())
    }

    fun computeHash(hash: (ByteArray) -> ByteArray): ByteArray {
        return hash(getRawData())
    }

    fun isFullySigned(): Boolean = senders.all { sender ->
        signatures.any { it.signerId == sender }
    }

    fun verifySignatures(verify: (ByteArray, ByteArray, ByteArray) -> Boolean, hash: (ByteArray) -> ByteArray): Boolean {
        val h = computeHash(hash)
        return signatures.all { sig -> verify(h, sig.signature, sig.publicKey) }
    }
}