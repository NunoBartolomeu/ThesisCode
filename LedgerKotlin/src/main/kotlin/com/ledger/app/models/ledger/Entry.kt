package com.ledger.app.models.ledger

data class Entry(
    val id: String,
    val timestamp: Long,
    val content: String,
    val senders: List<String>,
    val recipients: List<String>,

    val hash: String,
    val signatures: List<Signature>,

    val ledgerName: String,
    val pageNum: Int? = null,

    val relatedEntries: List<String>,
    val keywords: List<String>,
) {
    data class Signature(
        val signerId: String,
        val publicKey: String,
        val signature: String,
        val algorithm: String
    )

    fun isFullySigned(): Boolean {
        return senders.all { sender ->
            signatures.any { it.signerId == sender }
        }
    }

    fun verifySignatures(
        verifyFunction: (data: String, publicKey: String, signature: String) -> Boolean
    ): Boolean {
        return signatures.count() == senders.count() &&
                signatures.all { verifyFunction(hash, it.signature, it.publicKey) }
    }
}

class EntryBuilder(
    private val hashFunction: (String) -> String
) {
    private var id: String? = null
    private var timestamp: Long? = null
    private var content: String? = null
    private val senders = mutableListOf<String>()
    private val recipients = mutableListOf<String>()
    private var ledgerName: String?= null

    constructor(
        hashFunction: (String) -> String,
        id: String,
        timestamp: Long,
        content: String,
        senders: List<String>,
        recipients: List<String> = emptyList(),
        ledgerName: String
    ) : this(hashFunction) {
        this.id = id
        this.timestamp = timestamp
        this.content = content
        this.senders.addAll(senders)
        this.recipients.addAll(recipients)
        this.ledgerName = ledgerName
    }

    fun id(id: String) = apply { this.id = id }

    fun timestamp(timestamp: Long) = apply { this.timestamp = timestamp }

    fun content(content: String) = apply { this.content = content }

    fun addSender(sender: String) = apply { this.senders.add(sender) }

    fun senders(senders: List<String>) = apply {
        this.senders.clear()
        this.senders.addAll(senders)
    }

    fun addRecipient(recipient: String) = apply { this.recipients.add(recipient) }

    fun recipients(recipients: List<String>) = apply {
        this.recipients.clear()
        this.recipients.addAll(recipients)
    }

    fun ledgerName(ledgerName: String) = apply { this.ledgerName = ledgerName }

    /**
     * Build the Entry with automatic hash computation
     */
    fun build(): Entry {
        val entryId = id ?:                 throw IllegalStateException("Entry ID is required")
        val entryTimestamp = timestamp ?:   throw IllegalStateException("Entry timestamp is required")
        val entryContent = content?:        throw IllegalStateException("Entry content is required")
        val entryLedgerName = ledgerName ?: throw IllegalStateException("Ledger name is required")
        if (senders.isEmpty())              throw IllegalStateException("At least one sender is required")

        val hash = hashFunction(listOf(
            entryId,
            entryTimestamp.toString(),
            entryContent,
            senders.joinToString(","),
            recipients.joinToString(",")
        ).joinToString("|"))

        return Entry(
            id = entryId,
            timestamp = entryTimestamp,
            content = entryContent,
            senders = senders.toList(),
            recipients = recipients.toList(),
            hash = hash,
            signatures = emptyList(),
            ledgerName = entryLedgerName,
            pageNum = null,
            relatedEntries = emptyList(),
            keywords = emptyList(),
        )
    }
}
