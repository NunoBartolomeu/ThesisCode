
package com.ledger.app.models.ledger

data class PageSummary(
    val ledgerName: String,
    val number: Int,
    val timestamp: Long,
    val previousHash: String?,
    val merkleRoot: String,
    val entryCount: Int,
    val entryIds: List<String>
)

data class Page(
    val ledgerName: String,
    val number: Int,
    val timestamp: Long,
    val previousHash: String?,
    val merkleRoot: String,
    val hash: String,
    val entries: List<Entry>,
) {
    fun toPageSummary() = PageSummary(
        ledgerName = ledgerName,
        number = number,
        timestamp = timestamp,
        previousHash = previousHash,
        merkleRoot = merkleRoot,
        entryCount = entries.count(),
        entryIds = entries.map { it.id }
    )
}

class PageBuilder(
    private val hashFunction: (String) -> String
) {
    private var ledgerName: String? = null
    private var number: Int? = null
    private var timestamp: Long? = null
    private var previousHash: String? = null
    private val entries = mutableListOf<Entry>()

    constructor(
        hashFunction: (String) -> String,
        ledgerName: String,
        number: Int,
        timestamp: Long = System.currentTimeMillis(),
        previousHash: String? = null,
        entries: List<Entry>
    ) : this(hashFunction) {
        this.ledgerName = ledgerName
        this.number = number
        this.timestamp = timestamp
        this.previousHash = previousHash
        this.entries.addAll(entries)
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

    private fun computeMerkleRoot(): String {
        var layer = entries.map { it.hash }
        while (layer.size > 1) {
            val nextLayer = mutableListOf<String>()
            for (i in layer.indices step 2) {
                val left = layer[i]
                val right = if (i + 1 < layer.size) layer[i + 1] else left
                nextLayer.add(hashFunction("$left|$right"))
            }
            layer = nextLayer
        }
        return layer[0]
    }

    fun build(): Page {
        val pageLedgerName = ledgerName ?: throw IllegalStateException("Ledger name is required")
        val pageNumber = number         ?: throw IllegalStateException("Page number is required")
        val pageTimestamp = timestamp   ?: throw IllegalStateException("Page timestamp is required")
        if (entries.isEmpty())             throw IllegalStateException("Entries can't be empty")

        val merkleRoot = computeMerkleRoot()

        val hash = hashFunction(listOf(
            pageLedgerName,
            pageNumber.toString(),
            pageTimestamp,
            previousHash,
            merkleRoot,
        ).joinToString("|"))

        return Page(
            ledgerName = pageLedgerName,
            number = pageNumber,
            timestamp = pageTimestamp,
            previousHash = previousHash,
            merkleRoot = merkleRoot,
            entries = entries.toList(),
            hash = hash
        )
    }
}