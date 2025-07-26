package com.ledger.app.models.ledger

import com.ledger.app.utils.CryptoProvider
import com.ledger.app.utils.HashProvider
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

data class LedgerConfig(
    val name: String,
    val entriesPerPage: Int,
    val hashAlgorithm: String,
    val cryptoAlgorithm: String
)

class Ledger(
    val config: LedgerConfig,
    private val hashProvider: HashProvider,
    private val cryptoProvider: CryptoProvider,
) {
    init {
        require(hashProvider.algorithm == config.hashAlgorithm)
        require(cryptoProvider.algorithm == config.cryptoAlgorithm)
    }

    val pages = CopyOnWriteArrayList<Page>()
    val holdingArea = CopyOnWriteArrayList<Entry>()
    val verifiedEntries = CopyOnWriteArrayList<Entry>()

    fun createEntry(content: String, senders: List<String>, recipients: List<String>, relatedEntries: List<String>, keywords: List<String>): Entry {
        val builder = EntryBuilder(
            hashFunction = { data -> hashProvider.toHashString(hashProvider.hash(data)) },
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = content,
            senders = senders,
            recipients = recipients,
            ledgerName = this.config.name,
        )
        val entry = builder.build().copy(relatedEntries = relatedEntries, keywords = keywords)
        holdingArea.add(entry)
        return entry
    }

    fun addSignature(entryId: String, sig: Entry.Signature): Entry {
        val entry = holdingArea.find { it.id == entryId } ?: throw Exception("Entry not found")
        if (!entry.senders.contains(sig.signerId)) throw Exception("Sender is not present in entry")

        val signatures = entry.signatures.toMutableList()
        if (signatures.any { it.signerId == sig.signerId }) return entry
        if (!cryptoProvider.verify(entry.hash, sig.signature, sig.publicKey)) throw Exception("Signature could not be verified")
        signatures.add(sig)
        val newEntry = entry.copy(signatures = signatures)

        updateEntry(newEntry)
        if (verifiedEntries.count() == config.entriesPerPage) {
            createPage()
        }

        return newEntry
    }

    fun verifyEntry(entry: Entry): Boolean {
        return entry.isFullySigned() && entry.verifySignatures(cryptoProvider::verify)
    }

    fun getEntryById(id: String): Entry? {
        return holdingArea.find { it.id == id } ?: verifiedEntries.find { it.id == id }
    }

    fun updateEntry(entry: Entry) {
        holdingArea.removeIf { it.id == entry.id }
        verifiedEntries.removeIf { it.id == entry.id }

        if (verifyEntry(entry)) {
            verifiedEntries.add(entry)
        } else {
            holdingArea.add(entry)
        }
    }

    fun getInclusionProof(entry: Entry): List<ByteArray> {
        if (entry.pageNum == null || entry.pageNum > pages.last().number)
            return emptyList()
        val page = pages[entry.pageNum]
        if (!page.entries.contains(entry))
            return emptyList()
        return page.entries.map { hashProvider.toHashByteArray(it.hash) }
    }

    fun createPage() {
        if (verifiedEntries.size < config.entriesPerPage) throw IllegalStateException("Not enough entries to create a page")
        val toAdd = verifiedEntries.take(config.entriesPerPage).sortedBy { it.timestamp }
        val builder = PageBuilder(
            hashFunction = { data -> hashProvider.toHashString(hashProvider.hash(data)) },
            ledgerName = this.config.name,
            number = pages.size,
            timestamp = System.currentTimeMillis(),
            previousHash = pages.lastOrNull()?.hash ?: "",
            entries = toAdd,
        )
        pages.add(builder.build())
        verifiedEntries.removeAll(toAdd)
    }
}
