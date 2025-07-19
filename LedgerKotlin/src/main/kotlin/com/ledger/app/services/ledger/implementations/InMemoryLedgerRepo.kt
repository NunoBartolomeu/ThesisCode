package com.ledger.app.services.ledger.implementations

import com.ledger.app.services.ledger.LedgerRepo
import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.LedgerConfig
import com.ledger.app.models.ledger.Page
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Repository
class InMemoryLedgerRepo : LedgerRepo {
    // Ledger configurations
    private val configs = ConcurrentHashMap<String, LedgerConfig>()

    // Pages per ledger: map of (ledgerName → (pageNumber → Page))
    private val pages = ConcurrentHashMap<String, ConcurrentHashMap<Int, Page>>()

    // Entries by ID, and index for lookups by ledger
    private val entriesById = ConcurrentHashMap<String, Entry>()
    private val entriesByLedger = ConcurrentHashMap<String, CopyOnWriteArrayList<Entry>>()

    override fun saveLedgerConfig(config: LedgerConfig): Boolean {
        return configs.putIfAbsent(config.name, config) == null
    }

    override fun getLedgerConfig(name: String): LedgerConfig? {
        return configs[name]
    }

    override fun getAllLedgersNames(): List<String> {
        return configs.keys().toList()
    }

    override fun savePage(page: Page): Boolean {
        val ledgerPages = pages.computeIfAbsent(page.ledgerName) { ConcurrentHashMap() }
        return ledgerPages.putIfAbsent(page.number, page) == null
    }

    override fun getPage(ledgerName: String, pageNumber: Int): Page? {
        return pages[ledgerName]?.get(pageNumber)
    }

    override fun getAllPages(ledgerName: String): List<Page> {
        return pages[ledgerName]?.values?.sortedBy { it.number } ?: emptyList()
    }

    override fun saveEntry(entry: Entry): Boolean {
        // store in flat map
        entriesById[entry.id] = entry
        // index in ledger list
        val list = entriesByLedger.computeIfAbsent(entry.ledgerName) { CopyOnWriteArrayList() }
        if (!list.any { it.id == entry.id }) {
            list.add(entry)
            return true
        }
        return false
    }

    override fun getEntry(entryId: String): Entry? {
        return entriesById[entryId]
    }

    override fun getEntriesByLedger(ledgerName: String): List<Entry> {
        return entriesByLedger[ledgerName]?.toList() ?: emptyList()
    }

    override fun getHoldingEntries(ledgerName: String): List<Entry> {
        return entriesByLedger[ledgerName]?.filter { it.pageNum == null } ?: emptyList()
    }

    override fun getVerifiedEntries(ledgerName: String): List<Entry> {
        return entriesByLedger[ledgerName]?.filter { it.pageNum != null } ?: emptyList()
    }
}
