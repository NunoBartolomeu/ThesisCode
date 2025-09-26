package com.ledger.app.models.ledger

import com.ledger.app.utils.hash.HashProvider

data class Page(
    val ledgerName: String,
    val number: Int,
    val timestamp: Long,
    val previousHash: String?,
    val merkleRoot: String,
    val hash: String,
    val entries: MutableList<Entry>,
) {
    fun updateEntryForDeletionOrRestoration(updatedEntry: Entry) {
        val index = entries.indexOfFirst { it.id == updatedEntry.id }
        if (index == -1) throw Exception("Entry not found")
        val currentEntry = entries[index]
        if (updatedEntry.isDeleted() && currentEntry.isDeleted() ||
            !updatedEntry.isDeleted() && !currentEntry.isDeleted()
            ) {
            throw Exception("Entries can only be updated for deletion and restoration")
        }
        entries[index] = updatedEntry
    }
}

class PageBuilder() {
    private var ledgerName: String? = null
    private var number: Int? = null
    private var timestamp: Long? = null
    private var previousHash: String? = null
    private val entries = mutableListOf<Entry>()

    private var hashAlgorithm: String? = null

    constructor(
        ledgerName: String,
        number: Int,
        timestamp: Long = System.currentTimeMillis(),
        previousHash: String? = null,
        entries: List<Entry>,
        hashAlgorithm: String
    ) : this() {
        this.ledgerName = ledgerName
        this.number = number
        this.timestamp = timestamp
        this.previousHash = previousHash
        this.entries.addAll(entries)
        this.hashAlgorithm = hashAlgorithm
    }

    fun ledgerName(ledgerName: String) = apply { this.ledgerName = ledgerName }

    fun number(number: Int) = apply { this.number = number }

    fun timestamp(timestamp: Long) = apply { this.timestamp = timestamp }

    fun previousHash(previousHash: String?) = apply { this.previousHash = previousHash }

    fun addEntry(entry: Entry) = apply { this.entries.add(entry) }

    fun entries(entries: List<Entry>) = apply {
        this.entries.clear()
        this.entries.addAll(entries)
    }

    companion object {
        fun computeMerkleTree(entries: List<Entry>, hashAlgorithm: String): List<List<String>> {
            if (entries.isEmpty()) throw IllegalStateException("Cannot compute Merkle tree for empty entries")

            val layers = mutableListOf<List<String>>()
            var currentLevel = entries.map { it.hash }
            layers.add(currentLevel.toList())

            while (currentLevel.size > 1) {
                val nextLevel = mutableListOf<String>()
                for (i in currentLevel.indices step 2) {
                    val left = currentLevel[i]
                    val right = if (i + 1 < currentLevel.size) currentLevel[i + 1] else left
                    nextLevel.add(HashProvider.toHexString(HashProvider.hash("$left|$right", hashAlgorithm)))
                }
                currentLevel = nextLevel
                layers.add(nextLevel.toList())
            }

            return layers
        }

        fun computeHash(
            ledgerName: String,
            number: Int,
            timestamp: Long,
            previousHash: String?,
            merkleRoot: String,
            hashAlgorithm: String
        ): String {
            return HashProvider.toHexString(HashProvider.hash(listOf(
                ledgerName,
                number.toString(),
                timestamp.toString(),
                previousHash,
                merkleRoot,
            ).joinToString("|"), hashAlgorithm))
        }
    }

    fun build(): Page {
        val pageLedgerName = ledgerName     ?: throw IllegalStateException("Ledger name is required")
        val pageNumber = number             ?: throw IllegalStateException("Page number is required")
        val pageTimestamp = timestamp       ?: throw IllegalStateException("Page timestamp is required")
        val hashAlgorithm = hashAlgorithm   ?: throw IllegalStateException("Hash algorithm required")
        if (entries.isEmpty())                 throw IllegalStateException("Entries can't be empty")

        val merkleTree = computeMerkleTree(entries, hashAlgorithm)
        val merkleRoot = merkleTree[merkleTree.size - 1][0]

        val hash = computeHash(
            ledgerName = pageLedgerName,
            number = pageNumber,
            timestamp = pageTimestamp,
            previousHash = previousHash,
            merkleRoot = merkleRoot,
            hashAlgorithm = hashAlgorithm
        )

        return Page(
            ledgerName = pageLedgerName,
            number = pageNumber,
            timestamp = pageTimestamp,
            previousHash = previousHash,
            merkleRoot = merkleRoot,
            entries = entries,
            hash = hash
        )
    }
}