package com.ledger.app.models.ledger

import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

data class LedgerConfig(
    val name: String,
    val entriesPerPage: Int,
    val hashAlgorithm: String
)

data class Ledger(
    val config: LedgerConfig,

    //TODO: make this more simple and make the service protect the full ledger against concurrent access
    val pages: MutableList<Page> = CopyOnWriteArrayList(),
    val holdingArea: CopyOnWriteArrayList<Entry> = CopyOnWriteArrayList(),
    val verifiedEntries: CopyOnWriteArrayList<Entry> = CopyOnWriteArrayList()
) {
    fun createEntry(content: String, senders: List<String>, recipients: List<String>, relatedEntries: List<String>, keywords: List<String>): Entry {
        val builder = EntryBuilder(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = content,
            senders = senders,
            recipients = recipients,
            ledgerName = this.config.name,
            hashAlgorithm = config.hashAlgorithm
        )
        val entry = builder.build().copy(relatedEntries = relatedEntries, keywords = keywords)
        holdingArea.add(entry)
        return entry
    }

    fun addSignature(entryId: String, sig: Entry.Signature): Entry {
        val entry = holdingArea.find { it.id == entryId } ?: throw Exception("Entry not found")
        if (entry.isDeleted()) throw Exception("Entry was deleted, no signatures can be added")
        if (!entry.senders.contains(sig.signerId)) throw Exception("Sender is not present in entry")

        val signatures = entry.signatures.toMutableList()
        if (signatures.any { it.signerId == sig.signerId }) return entry
        if (!SignatureProvider.verify(
                data = entry.hash,
                hexSignature = sig.signatureData,
                hexPublicKey = sig.publicKey,
                algorithm = sig.algorithm
            )) throw Exception("Signature could not be verified")
        signatures.add(sig)
        val newEntry = entry.copy(signatures = signatures)

        updateEntry(newEntry)
        if (verifiedEntries.count() == config.entriesPerPage) {
            createPage()
        }

        return newEntry
    }

    fun deleteEntry(entryId: String): Entry {
        val entry = getEntryById(entryId) ?: throw Exception("Entry not found")
        val contentHash = HashProvider.toHashString(HashProvider.hash(entry.content, config.hashAlgorithm))
        val deletedContent = "DELETED_ENTRY\ncontent_hash:$contentHash\nentry_hash:${entry.hash}"
        val deletedEntry = entry.copy(content = deletedContent)
        updateEntry(deletedEntry)
        return deletedEntry
    }

    fun restoreEntry(entryId: String, originalContent: String): Entry {
        val entry = getEntryById(entryId) ?: throw Exception("Entry not found")
        if (!entry.isDeleted()) throw Exception("Entry is not deleted")

        val storedContentHash = Regex("content_hash:([^\n]+)").find(entry.content)?.groupValues?.get(1)
            ?: throw Exception("Invalid deleted entry format - missing content hash")
        val storedEntryHash = Regex("entry_hash:([^\n]+)").find(entry.content)?.groupValues?.get(1)
            ?: throw Exception("Invalid deleted entry format - missing entry hash")
        val originalContentHash = HashProvider.toHashString(HashProvider.hash(originalContent, config.hashAlgorithm))

        if (originalContentHash != storedContentHash) {
            throw Exception("Original content does not match stored hash")
        }

        val builder = EntryBuilder(
            id = entry.id,
            timestamp = entry.timestamp,
            content = originalContent,
            senders = entry.senders,
            recipients = entry.recipients,
            ledgerName = entry.ledgerName,
            hashAlgorithm = config.hashAlgorithm
        )

        val restoredEntry = builder.build().copy(
            signatures = entry.signatures,
            relatedEntries = entry.relatedEntries,
            keywords = entry.keywords
        )

        if (restoredEntry.hash != storedEntryHash) {
            throw Exception("Restored entry hash does not match original entry hash - entry may have been tampered with")
        }

        updateEntry(restoredEntry)
        return restoredEntry
    }

    fun getEntryById(id: String): Entry? {
        return holdingArea.find { it.id == id }
            ?: verifiedEntries.find { it.id == id }
            ?: pages.flatMap { it.entries }.find { it.id == id }
    }

    fun updateEntry(entry: Entry) {
        holdingArea.removeIf { it.id == entry.id }
        verifiedEntries.removeIf { it.id == entry.id }

        if (entry.verify()) {
            verifiedEntries.add(entry)
        } else {
            holdingArea.add(entry)
        }
    }

    fun getInclusionProof(entry: Entry): List<String> {
        val page = pages[entry.pageNum ?: throw Exception("Entry is not yet in a page")]
        val merkleTree = PageBuilder.computeMerkleTree(page.entries, config.hashAlgorithm)
        val proof = mutableListOf<String>()

        var index = page.entries.indexOfFirst { it.id == entry.id }
        if (index == -1) throw Exception("Entry not found in page entries")

        for (layer in merkleTree.dropLast(1)) {
            val siblingIndex = if (index % 2 == 0) index + 1 else index - 1
            val siblingHash = if (siblingIndex < layer.size) layer[siblingIndex] else layer[index]
            proof.add(siblingHash)

            index /= 2
        }

        return proof
    }

    fun createPage() {
        if (verifiedEntries.size < config.entriesPerPage) throw IllegalStateException("Not enough entries to create a page")
        val toAdd = verifiedEntries.take(config.entriesPerPage).sortedBy { it.timestamp }
        val builder = PageBuilder(
            ledgerName = this.config.name,
            number = pages.size,
            timestamp = System.currentTimeMillis(),
            previousHash = pages.lastOrNull()?.hash,
            entries = toAdd,
            hashAlgorithm = config.hashAlgorithm
        )
        pages.add(builder.build())
        verifiedEntries.removeAll(toAdd)
    }
}