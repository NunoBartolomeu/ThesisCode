package com.ledger.app.models.ledger

import com.ledger.app.utils.CryptoProvider
import com.ledger.app.utils.HashProvider
import java.lang.IllegalStateException
import java.util.*

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

    val pages = mutableListOf<Page>()
    val holdingArea = mutableListOf<Entry>()
    val verifiedEntries = mutableListOf<Entry>()
    val entryMap = mutableMapOf<String, Entry>()

    fun createEntry(
        content: String,
        senders: List<String>,
        recipients: List<String>,
        relatedEntries: List<String>,
        keywords: List<String>
    ): Entry {
        require(senders.isNotEmpty())
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
        entryMap[entry.id] = entry
        return entry
    }

    fun addSignature(entryId: String, sig: Entry.Signature): Boolean {
        val entry = entryMap[entryId] ?: return false
        if (!entry.senders.contains(sig.signerId)) return false
        val signatures = entry.signatures.toMutableList()
        if (signatures.any { it.signerId == sig.signerId }) return true
        if (!cryptoProvider.verify(entry.hash, sig.signature, sig.publicKey)) return false
        signatures.add(sig)
        holdingArea.remove(entry)

        val newEntry = entry.copy(signatures = signatures)

        println("New count: ${newEntry.signatures.count()}, old count: ${entry.signatures.count()}")

        if (verifyEntry(newEntry)) {
            verifiedEntries.add(newEntry)
            println("Entries per page: ${config.entriesPerPage}, Verified entries: ${verifiedEntries.count()}, should create: ${verifiedEntries.count() == config.entriesPerPage}")
            if (verifiedEntries.count() == config.entriesPerPage) {
                createPage()
            }
        } else {
            holdingArea.add(newEntry)
        }
        return true
    }

    fun verifyEntry(entry: Entry): Boolean {
        return entry.isFullySigned() && entry.verifySignatures(cryptoProvider::verify)
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
