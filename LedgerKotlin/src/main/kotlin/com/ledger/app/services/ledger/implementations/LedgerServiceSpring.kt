package com.ledger.app.services.ledger.implementations

import com.ledger.app.services.sys_kp.implementations.SysKeyPairServiceLocal
import com.ledger.app.services.ledger.LedgerRepo
import com.ledger.app.services.ledger.LedgerService
import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Ledger
import com.ledger.app.models.ledger.LedgerConfig
import com.ledger.app.models.ledger.PageSummary
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.CryptoProvider
import com.ledger.app.utils.HashProvider
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.Rgb
import org.springframework.stereotype.Service

@Service
class LedgerServiceSpring (
    private val ledgerRepo: LedgerRepo,
    private val hashProvider: HashProvider,
    private val cryptoProvider: CryptoProvider,
    private val sysKeyPairServiceLocal: SysKeyPairServiceLocal
) : LedgerService {
    private val logger = ColorLogger("LedgerService", Rgb(150, 50, 50), LogLevel.DEBUG)
    private val activeLedgers = mutableMapOf<String, Ledger>()

    override fun createLedger(name: String, linesPerPage: Int, hashAlgorithm: String, cryptoAlgorithm: String): Boolean {
        val config = LedgerConfig(name, linesPerPage, hashAlgorithm, cryptoAlgorithm)
        val ledger = Ledger(config, hashProvider, cryptoProvider)
        val success = ledgerRepo.saveLedgerConfig(config)
        if (success) {
            logger.info("Ledger ${ledger.config.name} was created")
            activeLedgers[name] = ledger
            return true
        } else {
            logger.warn("Leger ${ledger.config.name} failed creation")
            return false
        }
    }

    override fun getAvailableLedgers(): List<String> {
        logger.debug("Getting available ledgers")
        return ledgerRepo.getAllLedgersNames()
    }

    private fun createPage(ledger: Ledger): Boolean {
        val page = ledger.createPage()
        if (page == null) {
            logger.warn("Failed to create new page for ledger ${ledger.config.name}")
            return false
        }
        logger.info("Successfully created page ${page.number} of ledger ${ledger.config.name}")
        ledgerRepo.savePage(page)
        return true
    }

    private fun getLedger(name: String): Ledger? {
        return activeLedgers[name] ?: run {
            val config = ledgerRepo.getLedgerConfig(name) ?: return null
            val ledger = Ledger(config, hashProvider, cryptoProvider)

            // Load all entries for this ledger
            val entries = ledgerRepo.getEntriesByLedger(name)
            for (entry in entries) {
                // Internal method to populate the ledger without triggering normal creation logic
                loadEntryIntoLedger(ledger, entry)
            }

            activeLedgers[name] = ledger
            ledger
        }
    }

    private fun loadEntryIntoLedger(ledger: Ledger, entry: Entry) {
        if (entry.pageNum == null)
            if (ledger.verifyEntry(entry)) {
                ledger.verifiedEntries.add(entry)
            } else {
                ledger.holdingArea.add(entry)
            }
        ledger.entryMap[entry.id] = entry
    }

    override fun createEntry(
        ledgerName: String,
        content: String,
        senders: List<String>,
        recipients: List<String>,
        relatedEntries: List<String>,
        keywords: List<String>
    ): String? {
        val ledger = getLedger(ledgerName) ?: return logger.warn("Ledger $ledgerName not found").let { null }
        val entry = ledger.createEntry(content, senders, recipients, relatedEntries, keywords)
        ledgerRepo.saveEntry(entry)
        logger.info("Entry ${entry.id} was inserted in Ledger $ledgerName")
        return entry.id
    }

    override fun signEntry(entryId: String, signerId: String, signature: ByteArray, publicKey: ByteArray): Boolean {
        val entry = ledgerRepo.getEntry(entryId) ?: return logger.warn("Entry $entryId not found").let { false }
        val ledger = getLedger(entry.ledgerName) ?: return logger.warn("Ledger ${entry.ledgerName} not found").let { false }

        val sig = Entry.Signature(signerId, publicKey, signature)
        val result = ledger.addSignature(entryId, sig)
        if (result) {
            ledgerRepo.saveEntry(entry)
        }
        if (ledger.verifiedEntries.size >= ledger.config.entriesPerPage)
            createPage(ledger)
        logger.info("Signature was added to entry ${entry.id}")
        return result
    }

    override fun addKeywords(entryId: String, keywords: List<String>) {
        val entry = ledgerRepo.getEntry(entryId) ?: return
        val ledger = getLedger(entry.ledgerName) ?: return

        // Add new keywords, avoiding duplicates
        keywords.forEach { k ->
            if (k !in entry.keywords) {
                entry.keywords.add(k)
            }
        }

        ledgerRepo.saveEntry(entry)
        ledger.entryMap[entryId] = entry
    }

    override fun removeKeyword(entryId: String, keyword: String) {
        val entry = ledgerRepo.getEntry(entryId) ?: return
        val ledger = getLedger(entry.ledgerName) ?: return

        entry.keywords.removeAll { it == keyword }

        ledgerRepo.saveEntry(entry)
        ledger.entryMap[entryId] = entry
    }

    override fun addRelatedEntries(entryId: String, relatedEntries: List<String>) {
        val entry = ledgerRepo.getEntry(entryId) ?: return
        val ledger = getLedger(entry.ledgerName) ?: return

        relatedEntries.forEach { rel ->
            if (rel !in entry.relatedEntries) {
                entry.relatedEntries.add(rel)
            }
        }

        ledgerRepo.saveEntry(entry)
        ledger.entryMap[entryId] = entry
    }

    override fun removeRelatedEntry(entryId: String, relatedEntry: String) {
        val entry = ledgerRepo.getEntry(entryId) ?: return
        val ledger = getLedger(entry.ledgerName) ?: return

        entry.relatedEntries.removeAll { it == relatedEntry }

        ledgerRepo.saveEntry(entry)
        ledger.entryMap[entryId] = entry
    }

    override fun getPageSummary(ledgerName: String, pageNumber: Int): PageSummary? {
        val page = ledgerRepo.getPage(ledgerName, pageNumber) ?: return null
        return page.toPageSummary()
    }

    override fun getMerkleProof(entryId: String): List<ByteArray> {
        val entry = ledgerRepo.getEntry(entryId) ?: return emptyList()
        val ledger = getLedger(entry.ledgerName) ?: return emptyList()
        return ledger.getInclusionProof(entry)
    }

    override fun getEntriesNeedingSignature(ledgerName: String, userId: String): List<Entry> {
        val ledger = getLedger(ledgerName) ?: return emptyList()
        return ledger.holdingArea.filter { it.senders.contains(userId) && it.signatures.none { s -> s.signerId == userId } }
    }

    override fun getEntriesBySender(ledgerName: String, userId: String): List<Entry> {
        val ledger = getLedger(ledgerName) ?: return emptyList()
        return ledger.entryMap.values.filter { it.senders.contains(userId) }
    }

    override fun getEntriesByRecipient(ledgerName: String, userId: String): List<Entry> {
        val ledger = getLedger(ledgerName) ?: return emptyList()
        return ledger.entryMap.values.filter { it.recipients.contains(userId) }
    }

    override fun getEntriesByKeyword(
        ledgerName: String,
        keyword: String
    ): List<Entry> {
        val ledger = getLedger(ledgerName) ?: return emptyList()
        return ledger.entryMap.values.filter { it.keywords.contains(keyword) }
    }

    override fun getRelatedEntries(entryId: String): List<Entry> {
        val entry = ledgerRepo.getEntry(entryId) ?: return emptyList()
        val ledger = getLedger(entry.ledgerName) ?: return emptyList()
        return entry.relatedEntries.mapNotNull { relatedId -> ledger.entryMap[relatedId] }
    }

    override fun validatePage(ledgerName: String, pageNumber: Int): Boolean {
        val page = ledgerRepo.getPage(ledgerName, pageNumber) ?: return false

        // Verify merkle root
        val ledger = getLedger(ledgerName) ?: return false
        // Implementation would validate that the page's merkle root matches the computed root
        // from the entries in the page

        // Verify link to previous page
        if (pageNumber > 0) {
            val prevPage = ledgerRepo.getPage(ledgerName, pageNumber - 1) ?: return false
            val prevHash = prevPage.computeHash(hashProvider::hash)
            if (!prevHash.contentEquals(page.previousHash)) {
                return false
            }
        }

        return true
    }

    override fun validateChain(ledgerName: String): Boolean {
        val pages = ledgerRepo.getAllPages(ledgerName)
        if (pages.isEmpty()) return true

        // Check pages are in sequence and properly linked
        for (i in 1 until pages.size) {
            val prevPage = pages[i-1]
            val currPage = pages[i]

            // Verify page numbers are sequential
            if (currPage.number != prevPage.number + 1) return false

            // Verify hash chain
            val prevHash = prevPage.computeHash(hashProvider::hash)
            if (!prevHash.contentEquals(currPage.previousHash)) return false
        }

        // All pages validated
        return true
    }

    override fun logSystemEvent(
        ledgerName: String,
        declaringSystem: String,
        userId: String?,
        details: String
    ) {
        val entryId = createEntry(ledgerName, details, listOf(declaringSystem), if (userId!=null)listOf(userId) else listOf())!!
        val rawData = getRawEntryData(entryId)!!
        val hashedData = hashProvider.hash(rawData)
        val signature = cryptoProvider.sign(hashedData, sysKeyPairServiceLocal.getKeyPair().private)
        signEntry(entryId, declaringSystem, signature, sysKeyPairServiceLocal.getKeyPair().public.encoded)
    }

    override fun getRawEntryData(entryId: String): ByteArray? {
        val entry = ledgerRepo.getEntry(entryId) ?: return null
        return entry.getRawData()
    }
}