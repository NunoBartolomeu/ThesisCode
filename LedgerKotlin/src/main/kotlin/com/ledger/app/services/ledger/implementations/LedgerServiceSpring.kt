package com.ledger.app.services.ledger.implementations

import com.ledger.app.services.sys_kp.implementations.SysKeyPairServiceLocal
import com.ledger.app.services.ledger.LedgerRepo
import com.ledger.app.services.ledger.LedgerService
import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Ledger
import com.ledger.app.models.ledger.LedgerConfig
import com.ledger.app.models.ledger.Page
import com.ledger.app.models.ledger.PageSummary
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.CryptoProvider
import com.ledger.app.utils.HashProvider
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException

@Service
class LedgerServiceSpring (
    private val repo: LedgerRepo,
    private val hashProvider: HashProvider,
    private val cryptoProvider: CryptoProvider,
    private val sysKeyPairServiceLocal: SysKeyPairServiceLocal
) : LedgerService {
    private val logger = ColorLogger("LedgerService", RGB.RED_BRIGHT, LogLevel.DEBUG)
    private val activeLedgers = mutableMapOf<String, Ledger>()

    private fun loadLedger(ledgerName: String): Ledger? {
        val config = repo.getLedgerConfig(ledgerName) ?: return null
        val ledger = Ledger(config, hashProvider, cryptoProvider)

        val entries = repo.getEntriesByLedger(ledgerName)
        for (entry in entries) {
            loadEntryIntoLedger(ledger, entry)
        }

        activeLedgers[ledgerName] = ledger
        return ledger
    }

    private fun saveEntry(entry: Entry): Boolean {
        if (!repo.saveEntry(entry)) {
            logger.warn("Repo did not save the entry")
            return false
        }
        val ledger = getLedger(entry.ledgerName)
        ledger?.updateEntry(entry)
        return true
    }

    private fun savePage(page: Page): Boolean {
        if (repo.savePage(page)) {
            val ledger = getLedger(page.ledgerName)
            ledger?.let { it.pages[page.number] = page }
            return true
        }
        return false
    }

    override fun createLedger(name: String, linesPerPage: Int, hashAlgorithm: String, cryptoAlgorithm: String) {
        val config = LedgerConfig(name, linesPerPage, hashAlgorithm, cryptoAlgorithm)
        val ledger = Ledger(config, hashProvider, cryptoProvider)
        if (repo.saveLedgerConfig(config)) {
            logger.info("Ledger ${ledger.config.name} was created")
            activeLedgers[name] = ledger
        } else {
            logger.warn("Leger ${ledger.config.name} failed creation")
            throw IllegalStateException("Failed to save ledger")
        }
    }

    override fun createEntry(ledgerName: String, content: String, senders: List<String>, recipients: List<String>, relatedEntries: List<String>, keywords: List<String>): Entry? {
        val ledger = getLedger(ledgerName) ?: return logger.warn("Ledger $ledgerName not found").let { null }
        val entry = ledger.createEntry(content, senders, recipients, relatedEntries, keywords)
        return if (saveEntry(entry)) {
            logger.info("Entry ${entry.id} was inserted in Ledger $ledgerName")
            entry
        } else {
            logger.warn("Failed to save entry ${entry.id}")
            null
        }
    }

    @Transactional
    override fun signEntry(entryId: String, signerId: String, signature: String, publicKey: String, signingAlgorithm: String) {
        val entry = repo.getEntry(entryId) ?: return logger.warn("Entry $entryId not found")
        val ledger = getLedger(entry.ledgerName) ?: return logger.warn("Ledger ${entry.ledgerName} not found")

        val lastPageNumber = ledger.pages.lastOrNull()?.number ?: -100
        val sig = Entry.Signature(signerId, publicKey, signature, signingAlgorithm)
        val signedEntry = ledger.addSignature(entryId, sig)

        val newLastPageNumber = ledger.pages.lastOrNull()?.number ?: -100

        if (!repo.saveEntry(signedEntry)) {
            logger.warn("Failed to save signed entry ${entry.id} to repo")
            throw IllegalStateException("Failed to save entry")
        }

        logger.info("Signature was added to entry ${entry.id}")
        if (lastPageNumber != newLastPageNumber) {
            if (savePage(ledger.pages.last())) {
                logger.info("Page ${ledger.pages.last().number} was automatically created and saved")
            } else {
                logger.warn("Failed to save automatically created page ${ledger.pages.last().number}")
                throw IllegalStateException("Failed to save page")
            }
        }
    }

    override fun logSystemEvent(ledgerName: String, declaringSystem: String, userId: String?, details: String) {
        val entry = createEntry(ledgerName, details, listOf(declaringSystem), if (userId!=null)listOf(userId) else listOf())!!
        val signature = cryptoProvider.sign(entry.hash, sysKeyPairServiceLocal.getKeyPair().private)
        signEntry(
            entryId = entry.id,
            signerId = declaringSystem,
            signature = cryptoProvider.keyOrSigToString(signature),
            publicKey = cryptoProvider.keyOrSigToString(sysKeyPairServiceLocal.getKeyPair().public.encoded),
            signingAlgorithm = cryptoProvider.algorithm
        )
    }



    override fun addKeywords(entryId: String, keywords: List<String>) {
        val entry = repo.getEntry(entryId) ?: return
        getLedger(entry.ledgerName) ?: return

        val uniqueNewKeywords = keywords - entry.keywords.toSet()
        if (uniqueNewKeywords.isEmpty()) return

        val newEntry = entry.copy(keywords = entry.keywords + uniqueNewKeywords)
        saveEntry(newEntry)
    }

    override fun removeKeyword(entryId: String, keyword: String) {
        val entry = repo.getEntry(entryId) ?: return
        getLedger(entry.ledgerName) ?: return

        if (keyword !in entry.keywords) return

        val newEntry = entry.copy(keywords = entry.keywords - keyword)
        saveEntry(newEntry)
    }

    override fun addRelatedEntries(entryId: String, relatedEntries: List<String>) {
        val entry = repo.getEntry(entryId) ?: return
        getLedger(entry.ledgerName) ?: return

        val uniqueRelatedEntries = relatedEntries - entry.relatedEntries.toSet()
        if (uniqueRelatedEntries.isEmpty()) return

        val newEntry = entry.copy(relatedEntries = entry.relatedEntries + uniqueRelatedEntries)
        saveEntry(newEntry)
    }

    override fun removeRelatedEntry(entryId: String, relatedEntry: String) {
        val entry = repo.getEntry(entryId) ?: return
        getLedger(entry.ledgerName) ?: return

        if (relatedEntry !in entry.relatedEntries) return

        val newEntry = entry.copy(relatedEntries = entry.relatedEntries - relatedEntry)
        saveEntry(newEntry)
    }



    override fun getAvailableLedgers(): List<String> {
        logger.debug("Getting available ledgers")
        return repo.getAllLedgersNames()
    }

    override fun getLedger(ledgerName: String): Ledger? {
        return activeLedgers[ledgerName] ?: loadLedger(ledgerName)
    }

    private fun loadEntryIntoLedger(ledger: Ledger, entry: Entry) {
        if (entry.pageNum == null)
            if (ledger.verifyEntry(entry)) {
                ledger.verifiedEntries.add(entry)
            } else {
                ledger.holdingArea.add(entry)
            }
        ledger.updateEntry(entry)
    }




    override fun validateChain(ledgerName: String): Boolean {
        val pages = repo.getAllPages(ledgerName)
        if (pages.isEmpty()) return true

        for (i in 1 until pages.size) {
            val prevPage = pages[i-1]
            val currPage = pages[i]

            // Verify page numbers are sequential
            if (currPage.number != prevPage.number + 1) return false

            // Verify hash chain
            val prevHash = prevPage.hash
            if (!prevHash.contentEquals(currPage.previousHash)) return false
        }

        return true
    }

    override fun getPage(ledgerName: String, number: Int): Page? {
        val ledger = getLedger(ledgerName)
        return ledger?.pages[number]
    }

    override fun getPageSummary(ledgerName: String, pageNumber: Int): PageSummary? {
        val page = repo.getPage(ledgerName, pageNumber) ?: return null
        return page.toPageSummary()
    }




    override fun validatePage(ledgerName: String, pageNumber: Int): Boolean {
        val page = repo.getPage(ledgerName, pageNumber) ?: return false

        getLedger(ledgerName) ?: return false

        // Verify link to previous page
        if (pageNumber > 0) {
            val prevPage = repo.getPage(ledgerName, pageNumber - 1) ?: return false
            val prevHash = prevPage.hash
            if (!prevHash.contentEquals(page.previousHash)) {
                return false
            }
        }

        return true
    }

    override fun getEntry(entryId: String): Entry? {
        return repo.getEntry(entryId)
    }

    override fun getEntriesBySender(ledgerName: String, userId: String): List<Entry> {
        val ledger = getLedger(ledgerName) ?: throw IllegalArgumentException("No ledger with name $ledgerName")
        val entryMap = ledger.holdingArea + ledger.verifiedEntries
        return entryMap.filter { it.senders.contains(userId) }
    }

    override fun getEntriesByRecipient(ledgerName: String, userId: String): List<Entry> {
        val ledger = getLedger(ledgerName) ?: throw IllegalArgumentException("No ledger with name $ledgerName")
        val entryMap = ledger.holdingArea + ledger.verifiedEntries
        return entryMap.filter { it.recipients.contains(userId) }
    }

    override fun getEntriesByKeyword(ledgerName: String, keyword: String): List<Entry> {
        val ledger = getLedger(ledgerName) ?: throw IllegalArgumentException("No ledger with name $ledgerName")
        val entryMap = ledger.holdingArea + ledger.verifiedEntries
        return entryMap.filter { it.keywords.contains(keyword) }
    }

    override fun getRelatedEntries(entryId: String): List<Entry> {
        val entry = repo.getEntry(entryId) ?: return emptyList()
        val ledger = getLedger(entry.ledgerName) ?: return emptyList()
        return entry.relatedEntries.mapNotNull { relatedId -> ledger.getEntryById(relatedId) }
    }

    override fun getEntriesNeedingSignature(ledgerName: String, userId: String): List<Entry> {
        val ledger = getLedger(ledgerName) ?: throw IllegalArgumentException("No ledger with name $ledgerName")
        return ledger.holdingArea.filter { it.senders.contains(userId) && it.signatures.none { s -> s.signerId == userId } }
    }

    override fun getInclusionProof(entryId: String): List<ByteArray> {
        val entry = repo.getEntry(entryId) ?: return emptyList()
        val ledger = getLedger(entry.ledgerName) ?: return emptyList()
        return ledger.getInclusionProof(entry)
    }

    override fun getHasher(): HashProvider = hashProvider
}