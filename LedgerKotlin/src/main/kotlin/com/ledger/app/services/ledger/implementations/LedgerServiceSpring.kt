package com.ledger.app.services.ledger.implementations

import com.ledger.app.models.ledger.*
import com.ledger.app.repositories.ledger.LedgerRepo
import com.ledger.app.services.ledger.LedgerService
import com.ledger.app.services.pki.PublicKeyInfrastructureService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.RGB
import com.ledger.app.utils.signature.SignatureProvider
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class LedgerServiceSpring(
    private val repo: LedgerRepo,
    private val pkiService: PublicKeyInfrastructureService
) : LedgerService {
    @Value("\${app.logLevel:INFO}")
    private lateinit var logLevelStr: String
    private lateinit var logger: ColorLogger

    @PostConstruct
    fun initialize() {
        logger = ColorLogger("LedgerService", RGB.RED, logLevelStr)
    }
    private val activeLedgers = mutableMapOf<String, Ledger>()

    private fun loadLedger(ledgerName: String): Ledger? {
        val ledger = repo.readLedger(ledgerName) ?: return null
        activeLedgers[ledgerName] = ledger
        return ledger
    }

    private fun saveEntry(entry: Entry) {
        if (repo.readEntry(entry.id) == null) {
            repo.createEntry(entry)
        } else {
            repo.updateEntry(entry)
        }
    }

    private fun savePage(page: Page) {
        repo.createPage(page)
    }

    override fun createLedger(name: String, linesPerPage: Int, hashAlgorithm: String) {
        val config = LedgerConfig(name, linesPerPage, hashAlgorithm)
        val ledger = Ledger(config)
        repo.createLedger(ledger)
        activeLedgers[name] = ledger
        logger.info("Ledger ${ledger.config.name} was created")
    }

    override fun createEntry(
        ledgerName: String,
        content: String,
        senders: List<String>,
        recipients: List<String>,
        relatedEntries: List<String>,
        keywords: List<String>
    ): Entry? {
        val ledger = getLedger(ledgerName) ?: return logger.warn("Ledger $ledgerName not found").let { null }
        val entry = ledger.createEntry(content, senders, recipients, relatedEntries, keywords)
        saveEntry(entry)
        logger.info("Entry ${entry.id} was inserted in Ledger $ledgerName")
        return entry
    }

    override fun signEntry(
        entryId: String,
        signerId: String,
        signature: String,
        publicKey: String,
        signingAlgorithm: String
    ) {
        val entry = repo.readEntry(entryId) ?: return logger.warn("Entry $entryId not found")
        val ledger = getLedger(entry.ledgerName) ?: return logger.warn("Ledger ${entry.ledgerName} not found")

        val lastPageNumber = ledger.pages.lastOrNull()?.number ?: -100
        val sig = Entry.Signature(signerId, publicKey, signature, signingAlgorithm)
        val signedEntry = ledger.addSignature(entryId, sig)

        val newLastPageNumber = ledger.pages.lastOrNull()?.number ?: -100

        repo.updateEntry(signedEntry)
        logger.info("Signature was added to entry ${entry.id}")

        if (lastPageNumber != newLastPageNumber) {
            savePage(ledger.pages.last())
            logger.info("Page ${ledger.pages.last().number} was automatically created and saved")
        }
    }

    override fun eraseEntry(ledgerName: String, entryId: String, userId: String) {
        val ledger = getLedger(ledgerName) ?: return logger.warn("Ledger $ledgerName not found")
        ledger.eraseEntryContent(entryId, userId)
    }

    override fun restoreEntry(
        ledgerName: String,
        entryId: String,
        userId: String,
        originalContent: String
    ) {
        val ledger = getLedger(ledgerName) ?: return logger.warn("Ledger $ledgerName not found")
        ledger.restoreEntryContent(entryId, userId, originalContent)
    }

    override fun logSystemEvent(ledgerName: String, declaringService: String, userId: String?, details: String) {
        val entry = createEntry(
            ledgerName,
            details,
            listOf(declaringService),
            if (userId != null) listOf(userId) else listOf()
        )!!
        val signature = SignatureProvider.sign(entry.hash.toByteArray(), pkiService.getSystemKeyPair().private, null)
        signEntry(
            entryId = entry.id,
            signerId = declaringService,
            signature = SignatureProvider.toHexString(signature),
            publicKey = SignatureProvider.toHexString(pkiService.getSystemKeyPair().public.encoded),
            signingAlgorithm = SignatureProvider.getDefaultAlgorithm()
        )
    }


    override fun addKeywords(entryId: String, keywords: List<String>) {
        val entry = repo.readEntry(entryId) ?: return
        getLedger(entry.ledgerName) ?: return

        val uniqueNewKeywords = keywords - entry.keywords.toSet()
        if (uniqueNewKeywords.isEmpty()) return

        val newEntry = entry.copy(keywords = entry.keywords + uniqueNewKeywords)
        saveEntry(newEntry)
    }

    override fun removeKeyword(entryId: String, keyword: String) {
        val entry = repo.readEntry(entryId) ?: return
        getLedger(entry.ledgerName) ?: return

        if (keyword !in entry.keywords) return

        val newEntry = entry.copy(keywords = entry.keywords - keyword)
        saveEntry(newEntry)
    }

    override fun addRelatedEntries(entryId: String, relatedEntries: List<String>) {
        val entry = repo.readEntry(entryId) ?: return
        getLedger(entry.ledgerName) ?: return

        val uniqueRelatedEntries = relatedEntries - entry.relatedEntries.toSet()
        if (uniqueRelatedEntries.isEmpty()) return

        val newEntry = entry.copy(relatedEntries = entry.relatedEntries + uniqueRelatedEntries)
        saveEntry(newEntry)
    }

    override fun removeRelatedEntry(entryId: String, relatedEntry: String) {
        val entry = repo.readEntry(entryId) ?: return
        getLedger(entry.ledgerName) ?: return

        if (relatedEntry !in entry.relatedEntries) return

        val newEntry = entry.copy(relatedEntries = entry.relatedEntries - relatedEntry)
        saveEntry(newEntry)
    }


    override fun getAvailableLedgers(): List<String> {
        logger.debug("Getting available ledgers")
        return repo.getAllLedgers().map { it.name }
    }

    override fun getLedger(ledgerName: String): Ledger? {
        return activeLedgers[ledgerName] ?: loadLedger(ledgerName)
    }

    override fun getPage(ledgerName: String, number: Int): Page? {
        val ledger = getLedger(ledgerName)
        return ledger?.pages?.toList()[number]
    }

    override fun getEntry(entryId: String): Entry? {
        return repo.readEntry(entryId)
    }

    override fun getEntriesBySender(ledgerName: String, userId: String): List<Entry> {
        val ledger = getLedger(ledgerName) ?: throw IllegalArgumentException("No ledger with name $ledgerName")
        val entryMap = ledger.unverifiedEntries + ledger.verifiedEntries
        return entryMap.map { it.value }.filter { it.senders.contains(userId) }
    }

    override fun getEntriesByRecipient(ledgerName: String, userId: String): List<Entry> {
        val ledger = getLedger(ledgerName) ?: throw IllegalArgumentException("No ledger with name $ledgerName")
        val entryMap = ledger.unverifiedEntries + ledger.verifiedEntries
        return entryMap.map { it.value }.filter { it.recipients.contains(userId) }
    }

    override fun getEntriesByKeyword(ledgerName: String, keyword: String): List<Entry> {
        val ledger = getLedger(ledgerName) ?: throw IllegalArgumentException("No ledger with name $ledgerName")
        val entryMap = ledger.unverifiedEntries + ledger.verifiedEntries
        return entryMap.map { it.value }.filter { it.keywords.contains(keyword) }
    }

    override fun getRelatedEntries(entryId: String): List<Entry> {
        val entry = repo.readEntry(entryId) ?: return emptyList()
        val ledger = getLedger(entry.ledgerName) ?: return emptyList()
        return entry.relatedEntries.mapNotNull { relatedId -> ledger.getEntryById(relatedId) }
    }

    override fun getEntriesNeedingSignature(ledgerName: String, userId: String): List<Entry> {
        val ledger = getLedger(ledgerName) ?: throw IllegalArgumentException("No ledger with name $ledgerName")
        return ledger.unverifiedEntries.map { it.value }.filter { it.senders.contains(userId) && it.signatures.none { s -> s.signerId == userId } }
    }

    override fun getReceipt(userId: String, entryId: String): Receipt {
        val entry = repo.readEntry(entryId) ?: throw IllegalArgumentException("No entry with id $entryId")
        val ledger = getLedger(entry.ledgerName) ?: throw IllegalArgumentException("No ledger with name ${entry.ledgerName}")
        return ledger.createReceipt(entryId, userId, pkiService.getSystemKeyPair(), SignatureProvider.getDefaultAlgorithm())
    }
}