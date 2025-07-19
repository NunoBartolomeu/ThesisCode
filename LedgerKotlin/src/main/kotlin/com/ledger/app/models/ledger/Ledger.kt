package com.ledger.app.models.ledger

import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Node
import com.ledger.app.utils.CryptoProvider
import com.ledger.app.utils.HashProvider
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
        val entry = Entry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = content.toByteArray(),
            senders = senders,
            recipients = recipients,
            signatures = mutableListOf(),
            relatedEntries = relatedEntries.toMutableList(),
            keywords = keywords.toMutableList(),
            ledgerName = this.config.name
        )
        holdingArea.add(entry)
        entryMap[entry.id] = entry
        return entry
    }

    fun addSignature(entryId: String, sig: Entry.Signature): Boolean {
        val entry = entryMap[entryId] ?: return false
        val entryHash = entry.computeHash(hashProvider::hash)
        if (!entry.senders.contains(sig.signerId)) return false
        if (!cryptoProvider.verify(entryHash, sig.signature, sig.publicKey)) return false
        if (entry.signatures.none { it.signerId == sig.signerId }) {
            entry.signatures.add(sig)
        }
        if (verifyEntry(entry)) {
            verifiedEntries.add(entry)
            holdingArea.remove(entry)
        }
        return true
    }

    fun verifyEntry(entry: Entry): Boolean {
        return entry.isFullySigned() && entry.verifySignatures(cryptoProvider::verify, hashProvider::hash)
    }

    fun getInclusionProof(entry: Entry): List<ByteArray> {
        if (entry.pageNum == null || entry.pageNum!! > pages.last().number)
            return emptyList()
        val page = pages[entry.pageNum!!]
        if (!page.entries.contains(entry))
            return emptyList()
        return page.entries.map { it.computeHash(hashProvider::hash) }
    }

    fun createPage(): Page? {
        if (verifiedEntries.size < config.entriesPerPage)
            return null
        val toAdd = verifiedEntries.take(config.entriesPerPage).sortedBy { it.timestamp }
        val merkleRoot = buildMerkleTree(toAdd.map { it.computeHash(hashProvider::hash) }).hash
        val prevHash = pages.lastOrNull()?.computeHash(hashProvider::hash)
        val page = Page(
            ledgerName = this.config.name,
            number = pages.size,
            previousHash = prevHash,
            entries = toAdd,
            merkleRoot = merkleRoot
        )
        pages.add(page)
        verifiedEntries.removeAll(toAdd)
        return page
    }

    private fun buildMerkleTree(hashes: List<ByteArray>): Node {
        if (hashes.isEmpty())
            throw Error("Cannot build empty merkle tree")
        val nodes = mutableListOf<Node>()
        var height = 0
        var layer = hashes
        while (layer.size > 1) {
            var index = 0
            layer.forEach {
                Node(
                    it,
                    height,
                    index,
                    nodes.find { it.height == height - 1 && it.index == index * 2 },
                    nodes.find { it.height == height - 1 && it.index == index * 2 + 1 }
                )
                index++
            }
            layer = layer.chunked(2).map {
                if (it.size == 1)
                    hashProvider.hash(it[0] + it[0])
                else
                    hashProvider.hash(it[0] + it[1])
            }
            height++
        }

        nodes.add(
            Node(
                layer.first(),
                height,
                0,
                nodes.find { it.height == height - 1 && it.index == 0 },
                nodes.find { it.height == height - 1 && it.index == 1 }
            )
        )

        return nodes.last()
    }
}
