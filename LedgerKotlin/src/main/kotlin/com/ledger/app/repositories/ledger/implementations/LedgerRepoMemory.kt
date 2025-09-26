package com.ledger.app.repositories.ledger.implementations

import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Ledger
import com.ledger.app.models.ledger.LedgerConfig
import com.ledger.app.models.ledger.Page
import com.ledger.app.repositories.ledger.LedgerRepo
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

//@Repository
class LedgerRepoMemory : LedgerRepo {
    private val ledgers = ConcurrentHashMap<String, Ledger>()
    private val pages = ConcurrentHashMap<String, Page>()
    private val entries = ConcurrentHashMap<String, Entry>()
    private val lock = ReentrantReadWriteLock()

    override fun getAllLedgers(): List<LedgerConfig> {
        return lock.read {
            ledgers.values.map { it.config }
        }
    }

    override fun createLedger(ledger: Ledger) {
        lock.write {
            if (ledgers.containsKey(ledger.config.name)) {
                throw IllegalArgumentException("Ledger with name '${ledger.config.name}' already exists")
            }
            ledgers[ledger.config.name] = ledger
        }
    }

    override fun readLedger(ledgerName: String): Ledger? {
        return lock.read {
            val baseLedger = ledgers[ledgerName] ?: return@read null

            // Get all pages for this ledger
            val ledgerPages = pages.values
                .filter { it.ledgerName == ledgerName }
                .sortedBy { it.number }
                .map { page ->
                    // Get all entries for this page
                    val pageEntries = entries.values
                        .filter { it.ledgerName == ledgerName && it.pageNum == page.number }
                        .sortedBy { it.timestamp }

                    page.copy(entries = pageEntries.toMutableList())
                }

            baseLedger.copy(pages = ConcurrentLinkedQueue<Page>().apply { addAll(ledgerPages) })
        }
    }

    override fun createPage(page: Page) {
        val pageKey = "${page.ledgerName}:${page.number}"
        lock.write {
            if (pages.containsKey(pageKey)) {
                throw IllegalArgumentException("Page ${page.number} for ledger '${page.ledgerName}' already exists")
            }
            pages[pageKey] = page

            page.entries.forEach { entry ->
                entries[entry.id] = entry.copy(pageNum = page.number)
            }
        }
    }

    override fun readPage(ledgerName: String, pageNumber: Int): Page? {
        val pageKey = "$ledgerName:$pageNumber"
        return lock.read {
            val basePage = pages[pageKey] ?: return@read null

            // Get all entries for this page
            val pageEntries = entries.values
                .filter { it.ledgerName == ledgerName && it.pageNum == pageNumber }
                .sortedBy { it.timestamp }

            basePage.copy(entries = pageEntries.toMutableList())
        }
    }

    override fun updatePageForTamperEvidenceTesting(page: Page) {
        val pageKey = "${page.ledgerName}:${page.number}"
        lock.write {
            pages[pageKey] = page

            page.entries.forEach { entry ->
                entries[entry.id] = entry.copy(pageNum = page.number)
            }
        }
    }

    override fun createEntry(entry: Entry) {
        lock.write {
            if (entries.containsKey(entry.id)) {
                throw IllegalArgumentException("Entry with ID '${entry.id}' already exists")
            }
            entries[entry.id] = entry
        }
    }

    override fun readEntry(entryId: String): Entry? {
        return entries[entryId]
    }

    override fun updateEntry(entry: Entry) {
        lock.write {
            entries[entry.id] = entry
        }
    }
}