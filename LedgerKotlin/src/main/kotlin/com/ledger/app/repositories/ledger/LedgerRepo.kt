package com.ledger.app.repositories.ledger

import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Ledger
import com.ledger.app.models.ledger.LedgerConfig
import com.ledger.app.models.ledger.Page

interface LedgerRepo {
    fun getAllLedgers(): List<LedgerConfig>
    fun createLedger(ledger: Ledger)
    fun readLedger(ledgerName: String): Ledger?

    fun createPage(page: Page)
    fun readPage(ledgerName: String, pageNumber: Int): Page?
    fun updatePageForTamperEvidenceTesting(page: Page) //TODO: THIS IS TO TEST THE WARDEN, REMOVE IN ACTUAL PROJECT

    fun createEntry(entry: Entry)
    fun readEntry(entryId: String): Entry?
    fun updateEntry(entry: Entry)
}