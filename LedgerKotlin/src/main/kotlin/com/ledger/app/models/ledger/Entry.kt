package com.ledger.app.models.ledger

import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider

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
        val signatureData: String,
        val algorithm: String
    )

    fun verify(): Boolean {
        return senders.all { sender -> signatures.any { it.signerId == sender } } &&
                signatures.all { sig ->
                    SignatureProvider.verify(hash, sig.signatureData, sig.publicKey, sig.algorithm)
                }
    }

    fun isDeleted(): Boolean {
        return content.startsWith("DELETED_ENTRY")
    }
}

class EntryBuilder() {
    private var id: String? = null
    private var timestamp: Long? = null
    private var content: String? = null
    private val senders = mutableListOf<String>()
    private val recipients = mutableListOf<String>()
    private var ledgerName: String?= null
    private var hashAlgorithm: String? = null

    private val keywords = mutableListOf<String>()

    private val relatedEntries = mutableListOf<String>()

    constructor(
        id: String,
        timestamp: Long,
        content: String,
        senders: List<String>,
        recipients: List<String> = emptyList(),
        ledgerName: String,
        hashAlgorithm: String,
        keywords: List<String> = emptyList(),
        relatedEntries: List<String> = emptyList(),
    ) : this() {
        this.id = id
        this.timestamp = timestamp
        this.content = content
        this.senders.addAll(senders)
        this.recipients.addAll(recipients)
        this.ledgerName = ledgerName
        this.hashAlgorithm = hashAlgorithm
        this.keywords.addAll(keywords)
        this.relatedEntries.addAll(relatedEntries)
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

    fun addKeyword(keyword: String) = apply { this.keywords.add(keyword) }

    fun addRelatedEntry(relatedEntry: String) = apply { this.keywords.add(relatedEntry) }

    companion object {
        fun computeHash(
            id: String,
            timestamp: Long,
            content: String,
            senders: List<String>,
            recipients: List<String>,
            hashAlgorithm: String
        ): String {
            return HashProvider.toHexString(HashProvider.hash(listOf(
                id,
                timestamp.toString(),
                content,
                senders.joinToString(","),
                recipients.joinToString(",")
            ).joinToString("|"), hashAlgorithm))
        }
    }

    fun build(): Entry {
        val entryId = id ?:                 throw IllegalStateException("Entry ID is required")
        val entryTimestamp = timestamp ?:   throw IllegalStateException("Entry timestamp is required")
        val entryContent = content?:        throw IllegalStateException("Entry content is required")
        val entryLedgerName = ledgerName ?: throw IllegalStateException("Ledger name is required")
        val hashAlgorithm = hashAlgorithm?: throw IllegalStateException("Hash algorithm is required")
        if (senders.isEmpty())              throw IllegalStateException("At least one sender is required")

        val hash = computeHash(
            id = entryId,
            timestamp = entryTimestamp,
            content = entryContent,
            senders = senders,
            recipients = recipients,
            hashAlgorithm = hashAlgorithm
        )

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
            relatedEntries = keywords,
            keywords = relatedEntries,
        )
    }
}