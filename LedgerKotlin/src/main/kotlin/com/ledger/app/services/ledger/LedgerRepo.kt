package com.ledger.app.services.ledger

import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.LedgerConfig
import com.ledger.app.models.ledger.Page

interface LedgerRepo {
    fun saveLedgerConfig(config: LedgerConfig): Boolean
    fun getLedgerConfig(name: String): LedgerConfig?

    fun getAllLedgersNames(): List<String>

    fun savePage(page: Page): Boolean
    fun getPage(ledgerName: String, pageNumber: Int): Page?
    fun getAllPages(ledgerName: String): List<Page>
    fun saveEntry(entry: Entry): Boolean
    fun getEntry(entryId: String): Entry?
    fun getEntriesByLedger(ledgerName: String): List<Entry>
    fun getHoldingEntries(ledgerName: String): List<Entry>
    fun getVerifiedEntries(ledgerName: String): List<Entry>
}