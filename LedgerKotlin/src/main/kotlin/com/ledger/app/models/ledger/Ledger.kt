package com.ledger.app.models.ledger

import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider
import java.lang.IllegalStateException
import java.security.KeyPair
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class LedgerConfig(
    val name: String,
    val entriesPerPage: Int,
    val hashAlgorithm: String
)

data class Ledger(
    val config: LedgerConfig,

    val pages: ConcurrentLinkedQueue<Page> = ConcurrentLinkedQueue(),
    val unverifiedEntries: ConcurrentHashMap<String, Entry> = ConcurrentHashMap(),
    val verifiedEntries: ConcurrentHashMap<String, Entry> = ConcurrentHashMap()
) {
    private val batchLock = ReentrantLock()


    ////// TESTING ONLY //////
    private inline fun <T> measureTimeMs(block: () -> T): Pair<T, Double> {
        val start = System.nanoTime()
        val result = block()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        return result to elapsedMs
    }
    val pageCreationTimesInMs = ConcurrentLinkedQueue<Double>()


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
        unverifiedEntries.putIfAbsent(entry.id, entry)
        return entry
    }

    fun addSignature(entryId: String, sig: Entry.Signature): Entry {
        if (unverifiedEntries[entryId] == null)
            throw Exception("Entry not found")
        unverifiedEntries.computeIfPresent(entryId) { _, entry ->
            if (entry.verify()) throw Exception("Entry is already fully signed and verified")
            if (entry.isDeleted()) throw Exception("Entry was deleted, no signatures can be added")
            if (!entry.senders.contains(sig.signerId)) throw Exception("Sender is not present in entry")

            val signatures = entry.signatures.toMutableList()
            if (signatures.any { it.signerId == sig.signerId }) throw Exception("Sender already signed entry")
            if (!SignatureProvider.verify(entry.hash, sig.signatureData,sig.publicKey,sig.algorithm))
                throw Exception("Signature could not be verified")

            signatures.add(sig)
            val newEntry = entry.copy(signatures = signatures)
            if (newEntry.verify()) {
                if (verifiedEntries[entryId] != null)
                    throw Exception("Entry was already verified")
                verifiedEntries[entryId] = newEntry
                tryToCreatePage()
                null
            } else {
                newEntry
            }
        }

        return unverifiedEntries[entryId]?: verifiedEntries[entryId]?: pages.flatMap { it.entries }.find { it.id == entryId }?: throw Exception("Entry disappeared")

    }

    private fun eraseEntryContentHelper(entry: Entry, userId: String): Entry {
        if (entry.isDeleted()) throw Exception("Entry is already deleted")
        if (!(entry.senders.contains(userId) || entry.recipients.contains(userId))) {
            throw Exception("Requester is not a participant of the entry")
        }
        val contentHash = HashProvider.toHexString(HashProvider.hash(entry.content, config.hashAlgorithm))
        val deletedContent = "DELETED_ENTRY\ncontent_hash:$contentHash\nentry_hash:${entry.hash}\ndeleted_by:$userId"
        val deletedEntry = entry.copy(content = deletedContent)

        return deletedEntry
    }

    fun eraseEntryContent(entryId: String, userId: String): Entry {
        if (unverifiedEntries.keys.contains(entryId)) {
            val deletedEntry = eraseEntryContentHelper(unverifiedEntries[entryId]!!, userId)
            unverifiedEntries[entryId] = deletedEntry
            return deletedEntry
        }
        else if (verifiedEntries.keys.contains(entryId)) {
            val deletedEntry = eraseEntryContentHelper(verifiedEntries[entryId]!!, userId)
            verifiedEntries[entryId] = deletedEntry
            return deletedEntry
        }
        else {
            val entry = pages.flatMap { it.entries }.find { it.id == entryId } ?: throw Exception("Entry not found")
            val deletedEntry = eraseEntryContentHelper(entry, userId)

            val page = pages.firstOrNull { it.number == entry.pageNum }
                ?: throw IllegalArgumentException("Page ${entry.pageNum} not found")

            page.entries.removeIf { it.id == entry.id }
            page.entries.add(deletedEntry)

            return deletedEntry
        }
    }

    fun restoreEntryContentHelper(entry: Entry, userId: String, originalContent: String): Entry {
        if (!entry.isDeleted()) throw Exception("Entry is not deleted")
        if (!(entry.senders.contains(userId) || entry.recipients.contains(userId))) {
            throw Exception("Requester is not a participant of the entry")
        }
        val storedContentHash = Regex("content_hash:([^\n]+)").find(entry.content)?.groupValues?.get(1)
            ?: throw Exception("Invalid deleted entry format - missing content hash")
        val storedEntryHash = Regex("entry_hash:([^\n]+)").find(entry.content)?.groupValues?.get(1)
            ?: throw Exception("Invalid deleted entry format - missing entry hash")
        val originalContentHash = HashProvider.toHexString(HashProvider.hash(originalContent, config.hashAlgorithm))

        if (originalContentHash != storedContentHash) {
            throw Exception("Original content does not match stored hash")
        }

        val restoredEntry = entry.copy(content = originalContent)
        return restoredEntry
    }

    fun restoreEntryContent(entryId: String, userId: String, originalContent: String): Entry {
        if (unverifiedEntries.keys.contains(entryId)) {
            val restoredEntry = restoreEntryContentHelper(unverifiedEntries[entryId]!!, userId, originalContent)
            unverifiedEntries[entryId] = restoredEntry
            return restoredEntry
        }
        else if (verifiedEntries.keys.contains(entryId)) {
            val restoredEntry = restoreEntryContentHelper(verifiedEntries[entryId]!!, userId, originalContent)
            verifiedEntries[entryId] = restoredEntry
            return restoredEntry
        }
        else {
            val entry = pages.flatMap { it.entries }.find { it.id == entryId } ?: throw Exception("Entry not found")
            val restoredEntry = restoreEntryContentHelper(entry, userId, originalContent)

            val page = pages.firstOrNull { it.number == entry.pageNum }
                ?: throw IllegalArgumentException("Page ${entry.pageNum} not found")

            page.entries.removeIf { it.id == entry.id }
            page.entries.add(restoredEntry)

            return restoredEntry
        }
    }

    fun getEntryById(id: String): Entry? {
        return unverifiedEntries[id]
            ?: verifiedEntries[id]
            ?: pages.flatMap { it.entries }.find { it.id == id }
    }

    fun getInclusionProof(entry: Entry): List<String> {
        val page = pages.find { it.number == entry.pageNum} ?: throw Exception("Entry is not yet in a page")
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

    fun tryToCreatePage() {
        if (verifiedEntries.size >= config.entriesPerPage) {
            batchLock.withLock {
                if (verifiedEntries.size >= config.entriesPerPage) {
                    val (_, time) = measureTimeMs {
                        val snapshot = mutableListOf<Entry>()
                        val keysToRemove = mutableListOf<String>()

                        verifiedEntries.forEach { (entryId, entry) ->
                            if (snapshot.size < config.entriesPerPage) {
                                snapshot.add(entry)
                                keysToRemove.add(entryId)
                            }
                        }

                        keysToRemove.forEach { verifiedEntries.remove(it) }
                        val pageNum = pages.size

                        val builder = PageBuilder(
                            ledgerName = this.config.name,
                            number = pageNum,
                            timestamp = System.currentTimeMillis(),
                            previousHash = pages.lastOrNull()?.hash,
                            entries = snapshot.map { it.copy(pageNum = pageNum) },
                            hashAlgorithm = config.hashAlgorithm
                        )
                        pages.add(builder.build())
                    }
                    pageCreationTimesInMs.add(time)
                }
            }
        }
    }

    fun createReceipt(entryId: String, requesterId: String, systemKeyPair: KeyPair, signatureAlgorithm: String): Receipt {
        val entry = getEntryById(entryId) ?: throw Exception("Entry not found")
        if (!(entry.senders.contains(requesterId) || entry.recipients.contains(requesterId))) {
            throw IllegalStateException("Requester is not a participant of the entry")
        }

        val proof = if (entry.pageNum == null) emptyList() else getInclusionProof(entry)

        val builder = ReceiptBuilder(
            entry = entry,
            timestamp = System.currentTimeMillis(),
            requesterId = requesterId,
            proof = proof,
            systemKeyPair = systemKeyPair,
            signatureAlgorithm = signatureAlgorithm,
            hashAlgorithm = config.hashAlgorithm
        )

        return builder.build()
    }
}