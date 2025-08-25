package com.ledger.app.services.ledger

import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Ledger
import com.ledger.app.models.ledger.Page
import com.ledger.app.models.ledger.PageSummary

interface LedgerService {
    fun createLedger(name: String, linesPerPage: Int, hashAlgorithm: String)
    fun createEntry(ledgerName: String, content: String, senders: List<String>, recipients: List<String>, relatedEntries: List<String> = emptyList(), keywords: List<String> = emptyList()): Entry?
    fun signEntry(entryId: String, signerId: String, signature: String, publicKey: String, signingAlgorithm: String)
    fun logSystemEvent(ledgerName: String, declaringService: String, userId: String?, details: String)

    fun addKeywords(entryId: String, keywords: List<String>)
    fun removeKeyword(entryId: String, keyword: String)
    fun addRelatedEntries(entryId: String, relatedEntries: List<String>)
    fun removeRelatedEntry(entryId: String, relatedEntry: String)

    fun getAvailableLedgers(): List<String>
    fun getLedger(ledgerName: String): Ledger?

    fun getPage(ledgerName: String, number: Int): Page?
    fun getPageSummary(ledgerName: String, pageNumber: Int): PageSummary?

    fun getEntry(entryId: String): Entry?
    fun getEntriesBySender(ledgerName: String, userId: String): List<Entry>
    fun getEntriesByRecipient(ledgerName: String, userId: String): List<Entry>
    fun getEntriesByKeyword(ledgerName: String, keyword: String): List<Entry>
    fun getRelatedEntries(entryId: String): List<Entry>
    fun getEntriesNeedingSignature(ledgerName: String, userId: String): List<Entry>

    fun getInclusionProof(entryId: String): List<String>
}