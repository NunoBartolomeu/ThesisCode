package com.ledger.app.services.ledger

import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Ledger
import com.ledger.app.models.ledger.Page
import com.ledger.app.models.ledger.PageSummary
import com.ledger.app.utils.HashProvider

interface LedgerService {
    fun createLedger(name: String, linesPerPage: Int, hashAlgorithm: String, cryptoAlgorithm: String): Boolean
    fun getAvailableLedgers(): List<String>

    fun createEntry(ledgerName: String, content: String, senders: List<String>, recipients: List<String>, relatedEntries: List<String> = emptyList(), keywords: List<String> = emptyList()): String?
    fun getRawEntryData(entryId: String): ByteArray?
    fun signEntry(entryId: String, signerId: String, signature: ByteArray, publicKey: ByteArray): Boolean
    fun addKeywords(entryId: String, keywords: List<String>)
    fun removeKeyword(entryId: String, keyword: String)
    fun addRelatedEntries(entryId: String, relatedEntries: List<String>)
    fun removeRelatedEntry(entryId: String, relatedEntry: String)
    fun getEntriesNeedingSignature(ledgerName: String, userId: String): List<Entry>
    fun getEntriesBySender(ledgerName: String, userId: String): List<Entry>
    fun getEntriesByRecipient(ledgerName: String, userId: String): List<Entry>
    fun getEntriesByKeyword(ledgerName: String, keyword: String): List<Entry>
    fun getRelatedEntries(entryId: String): List<Entry>

    fun getPageSummary(ledgerName: String, pageNumber: Int): PageSummary?
    fun getMerkleProof(entryId: String): List<ByteArray>
    fun validatePage(ledgerName: String, pageNumber: Int): Boolean
    fun validateChain(ledgerName: String): Boolean
    fun getLedgerWithPages(ledgerName: String): Ledger?
    fun getPage(ledgerName: String, number: Int): Page?
    fun getEntry(entryId: String): Entry?
    fun getHasher(): HashProvider


    fun logSystemEvent(ledgerName: String, declaringSystem: String, userId: String?, details: String)
}