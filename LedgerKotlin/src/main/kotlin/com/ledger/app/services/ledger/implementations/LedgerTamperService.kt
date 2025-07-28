package com.ledger.app.services.ledger.implementations

import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Page
import com.ledger.app.services.ledger.LedgerRepo
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class TamperState(
    val ledgerName: String,
    val pageNumber: Int,
    val entryId: String?,
    val tamperType: TamperType,
    val originalValue: String,
    val tamperedValue: String,
    val isCurrentlyTampered: Boolean
)

enum class TamperType {
    PAGE_TIMESTAMP,
    PAGE_HASH,
    PAGE_MERKLE_ROOT,
    PAGE_PREVIOUS_HASH,
    ENTRY_TIMESTAMP,
    ENTRY_HASH,
    ENTRY_CONTENT,
    SIGNATURE_VALUE,
    SIGNATURE_PUBLIC_KEY
}

@Service
class LedgerTamperingService(
    private val repo: LedgerRepo
) {
    private val logger = ColorLogger("LedgerTamperer", RGB.YELLOW_SOFT, LogLevel.DEBUG)

    // Track current tampering state for each ledger
    private val tamperStates = ConcurrentHashMap<String, TamperState>()

    @Scheduled(fixedDelayString = "\${ledger.tampering.interval:30000}") // Default 30 seconds
    fun performTamperingCycle() {
        logger.info("Starting tampering cycle")

        val ledgerConfigs = repo.getAllLedgers()

        for (config in ledgerConfigs) {
            try {
                val currentState = tamperStates[config.name]

                if (currentState?.isCurrentlyTampered == true) {
                    // Restore original data
                    restoreOriginalData(currentState)
                    logger.info("🔧 Restored ledger '${config.name}' to original state")
                } else {
                    // Introduce new tampering
                    val newTamperState = introduceTampering(config.name)
                    if (newTamperState != null) {
                        tamperStates[config.name] = newTamperState
                        logger.info("Introduced tampering in ledger '${config.name}': ${newTamperState.tamperType}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to perform tampering cycle for ledger '${config.name}': ${e.message}")
            }
        }

        logger.info("Completed tampering cycle for ${ledgerConfigs.size} ledgers")
    }

    private fun introduceTampering(ledgerName: String): TamperState? {
        val ledger = repo.readLedger(ledgerName) ?: return null

        if (ledger.pages.isEmpty()) {
            logger.warn("No pages found in ledger '$ledgerName' to tamper with")
            return null
        }

        val targetPage = ledger.pages.random()
        val tamperType = selectRandomTamperType(targetPage)

        return when (tamperType) {
            TamperType.PAGE_TIMESTAMP -> tamperPageTimestamp(targetPage)
            TamperType.PAGE_HASH -> tamperPageHash(targetPage)
            TamperType.PAGE_MERKLE_ROOT -> tamperPageMerkleRoot(targetPage)
            TamperType.PAGE_PREVIOUS_HASH -> tamperPagePreviousHash(targetPage)
            TamperType.ENTRY_TIMESTAMP -> tamperEntryTimestamp(targetPage)
            TamperType.ENTRY_HASH -> tamperEntryHash(targetPage)
            TamperType.ENTRY_CONTENT -> tamperEntryContent(targetPage)
            TamperType.SIGNATURE_VALUE -> tamperSignatureValue(targetPage)
            TamperType.SIGNATURE_PUBLIC_KEY -> tamperSignaturePublicKey(targetPage)
        }
    }

    private fun selectRandomTamperType(page: Page): TamperType {
        val pageTypes = listOf(
            TamperType.PAGE_TIMESTAMP,
            TamperType.PAGE_HASH,
            TamperType.PAGE_MERKLE_ROOT,
            TamperType.PAGE_PREVIOUS_HASH
        )

        val entryTypes = if (page.entries.isNotEmpty()) {
            listOf(
                TamperType.ENTRY_TIMESTAMP,
                TamperType.ENTRY_HASH,
                TamperType.ENTRY_CONTENT,
                TamperType.SIGNATURE_VALUE,
                TamperType.SIGNATURE_PUBLIC_KEY
            )
        } else {
            emptyList()
        }

        val allTypes = pageTypes + entryTypes
        return allTypes.random()
    }

    private fun tamperPageTimestamp(page: Page): TamperState {
        val originalValue = page.timestamp.toString()
        val tamperedTimestamp = page.timestamp + Random.nextLong(-5000, 5000)
        val tamperedValue = tamperedTimestamp.toString()

        val tamperedPage = page.copy(timestamp = tamperedTimestamp)
        repo.updatePageForTamperEvidenceTesting(tamperedPage)

        return TamperState(
            ledgerName = page.ledgerName,
            pageNumber = page.number,
            entryId = null,
            tamperType = TamperType.PAGE_TIMESTAMP,
            originalValue = originalValue,
            tamperedValue = tamperedValue,
            isCurrentlyTampered = true
        )
    }

    private fun tamperPageHash(page: Page): TamperState {
        val originalValue = page.hash
        val tamperedValue = modifyHashString(originalValue)

        val tamperedPage = page.copy(hash = tamperedValue)
        repo.updatePageForTamperEvidenceTesting(tamperedPage)

        return TamperState(
            ledgerName = page.ledgerName,
            pageNumber = page.number,
            entryId = null,
            tamperType = TamperType.PAGE_HASH,
            originalValue = originalValue,
            tamperedValue = tamperedValue,
            isCurrentlyTampered = true
        )
    }

    private fun tamperPageMerkleRoot(page: Page): TamperState {
        val originalValue = page.merkleRoot
        val tamperedValue = modifyHashString(originalValue)

        val tamperedPage = page.copy(merkleRoot = tamperedValue)
        repo.updatePageForTamperEvidenceTesting(tamperedPage)

        return TamperState(
            ledgerName = page.ledgerName,
            pageNumber = page.number,
            entryId = null,
            tamperType = TamperType.PAGE_MERKLE_ROOT,
            originalValue = originalValue,
            tamperedValue = tamperedValue,
            isCurrentlyTampered = true
        )
    }

    private fun tamperPagePreviousHash(page: Page): TamperState {
        val originalValue = page.previousHash ?: "null"
        val tamperedValue = if (originalValue == "null") {
            generateRandomHash()
        } else {
            modifyHashString(originalValue)
        }

        val tamperedPage = page.copy(previousHash = if (tamperedValue == "null") null else tamperedValue)
        repo.updatePageForTamperEvidenceTesting(tamperedPage)

        return TamperState(
            ledgerName = page.ledgerName,
            pageNumber = page.number,
            entryId = null,
            tamperType = TamperType.PAGE_PREVIOUS_HASH,
            originalValue = originalValue,
            tamperedValue = tamperedValue,
            isCurrentlyTampered = true
        )
    }

    private fun tamperEntryTimestamp(page: Page): TamperState? {
        if (page.entries.isEmpty()) return null

        val targetEntry = page.entries.random()
        val originalValue = targetEntry.timestamp.toString()
        val tamperedTimestamp = targetEntry.timestamp + Random.nextLong(-1000, 1000)
        val tamperedValue = tamperedTimestamp.toString()

        val tamperedEntry = targetEntry.copy(timestamp = tamperedTimestamp)
        repo.updateEntry(tamperedEntry)

        return TamperState(
            ledgerName = page.ledgerName,
            pageNumber = page.number,
            entryId = targetEntry.id,
            tamperType = TamperType.ENTRY_TIMESTAMP,
            originalValue = originalValue,
            tamperedValue = tamperedValue,
            isCurrentlyTampered = true
        )
    }

    private fun tamperEntryHash(page: Page): TamperState? {
        if (page.entries.isEmpty()) return null

        val targetEntry = page.entries.random()
        val originalValue = targetEntry.hash
        val tamperedValue = modifyHashString(originalValue)

        val tamperedEntry = targetEntry.copy(hash = tamperedValue)
        repo.updateEntry(tamperedEntry)

        return TamperState(
            ledgerName = page.ledgerName,
            pageNumber = page.number,
            entryId = targetEntry.id,
            tamperType = TamperType.ENTRY_HASH,
            originalValue = originalValue,
            tamperedValue = tamperedValue,
            isCurrentlyTampered = true
        )
    }

    private fun tamperEntryContent(page: Page): TamperState? {
        if (page.entries.isEmpty()) return null

        val targetEntry = page.entries.random()
        val originalValue = targetEntry.content
        val tamperedValue = modifyStringContent(originalValue)

        val tamperedEntry = targetEntry.copy(content = tamperedValue)
        repo.updateEntry(tamperedEntry)

        return TamperState(
            ledgerName = page.ledgerName,
            pageNumber = page.number,
            entryId = targetEntry.id,
            tamperType = TamperType.ENTRY_CONTENT,
            originalValue = originalValue,
            tamperedValue = tamperedValue,
            isCurrentlyTampered = true
        )
    }

    private fun tamperSignatureValue(page: Page): TamperState? {
        val entriesWithSignatures = page.entries.filter { it.signatures.isNotEmpty() }
        if (entriesWithSignatures.isEmpty()) return null

        val targetEntry = entriesWithSignatures.random()
        val targetSignature = targetEntry.signatures.random()
        val originalValue = targetSignature.signature
        val tamperedValue = modifySignatureString(originalValue)

        val tamperedSignature = targetSignature.copy(signature = tamperedValue)
        val tamperedSignatures = targetEntry.signatures.map {
            if (it.signerId == targetSignature.signerId) tamperedSignature else it
        }
        val tamperedEntry = targetEntry.copy(signatures = tamperedSignatures)
        repo.updateEntry(tamperedEntry)

        return TamperState(
            ledgerName = page.ledgerName,
            pageNumber = page.number,
            entryId = targetEntry.id,
            tamperType = TamperType.SIGNATURE_VALUE,
            originalValue = originalValue,
            tamperedValue = tamperedValue,
            isCurrentlyTampered = true
        )
    }

    private fun tamperSignaturePublicKey(page: Page): TamperState? {
        val entriesWithSignatures = page.entries.filter { it.signatures.isNotEmpty() }
        if (entriesWithSignatures.isEmpty()) return null

        val targetEntry = entriesWithSignatures.random()
        val targetSignature = targetEntry.signatures.random()
        val originalValue = targetSignature.publicKey
        val tamperedValue = modifyPublicKeyString(originalValue)

        val tamperedSignature = targetSignature.copy(publicKey = tamperedValue)
        val tamperedSignatures = targetEntry.signatures.map {
            if (it.signerId == targetSignature.signerId) tamperedSignature else it
        }
        val tamperedEntry = targetEntry.copy(signatures = tamperedSignatures)
        repo.updateEntry(tamperedEntry)

        return TamperState(
            ledgerName = page.ledgerName,
            pageNumber = page.number,
            entryId = targetEntry.id,
            tamperType = TamperType.SIGNATURE_PUBLIC_KEY,
            originalValue = originalValue,
            tamperedValue = tamperedValue,
            isCurrentlyTampered = true
        )
    }

    private fun restoreOriginalData(tamperState: TamperState) {
        val ledger = repo.readLedger(tamperState.ledgerName) ?: return
        val targetPage = ledger.pages.find { it.number == tamperState.pageNumber } ?: return

        when (tamperState.tamperType) {
            TamperType.PAGE_TIMESTAMP -> {
                val restoredPage = targetPage.copy(timestamp = tamperState.originalValue.toLong())
                repo.updatePageForTamperEvidenceTesting(restoredPage)
            }
            TamperType.PAGE_HASH -> {
                val restoredPage = targetPage.copy(hash = tamperState.originalValue)
                repo.updatePageForTamperEvidenceTesting(restoredPage)
            }
            TamperType.PAGE_MERKLE_ROOT -> {
                val restoredPage = targetPage.copy(merkleRoot = tamperState.originalValue)
                repo.updatePageForTamperEvidenceTesting(restoredPage)
            }
            TamperType.PAGE_PREVIOUS_HASH -> {
                val restoredValue = if (tamperState.originalValue == "null") null else tamperState.originalValue
                val restoredPage = targetPage.copy(previousHash = restoredValue)
                repo.updatePageForTamperEvidenceTesting(restoredPage)
            }
            TamperType.ENTRY_TIMESTAMP -> {
                restoreEntryField(targetPage, tamperState.entryId!!) { entry, originalValue ->
                    entry.copy(timestamp = originalValue.toLong())
                }
            }
            TamperType.ENTRY_HASH -> {
                restoreEntryField(targetPage, tamperState.entryId!!) { entry, originalValue ->
                    entry.copy(hash = originalValue)
                }
            }
            TamperType.ENTRY_CONTENT -> {
                restoreEntryField(targetPage, tamperState.entryId!!) { entry, originalValue ->
                    entry.copy(content = originalValue)
                }
            }
            TamperType.SIGNATURE_VALUE -> {
                restoreSignatureField(targetPage, tamperState.entryId!!, tamperState.originalValue) { signature, originalValue ->
                    signature.copy(signature = originalValue)
                }
            }
            TamperType.SIGNATURE_PUBLIC_KEY -> {
                restoreSignatureField(targetPage, tamperState.entryId!!, tamperState.originalValue) { signature, originalValue ->
                    signature.copy(publicKey = originalValue)
                }
            }
        }

        // Mark as restored
        tamperStates[tamperState.ledgerName] = tamperState.copy(isCurrentlyTampered = false)
    }

    private fun restoreEntryField(page: Page, entryId: String, restoreFunction: (Entry, String) -> Entry) {
        val targetEntry = page.entries.find { it.id == entryId } ?: return
        val tamperState = tamperStates[page.ledgerName] ?: return
        val restoredEntry = restoreFunction(targetEntry, tamperState.originalValue)
        repo.updateEntry(restoredEntry)
    }

    private fun restoreSignatureField(page: Page, entryId: String, originalValue: String, restoreFunction: (Entry.Signature, String) -> Entry.Signature) {
        val targetEntry = page.entries.find { it.id == entryId } ?: return
        val targetSignature = targetEntry.signatures.find {
            it.signature == tamperStates[page.ledgerName]?.tamperedValue ||
                    it.publicKey == tamperStates[page.ledgerName]?.tamperedValue
        } ?: return

        val restoredSignature = restoreFunction(targetSignature, originalValue)
        val restoredSignatures = targetEntry.signatures.map {
            if (it.signerId == targetSignature.signerId) restoredSignature else it
        }
        val restoredEntry = targetEntry.copy(signatures = restoredSignatures)
        repo.updateEntry(restoredEntry)
    }

    // Utility functions for creating small modifications
    private fun modifyHashString(hash: String): String {
        if (hash.isEmpty()) return "a"

        val chars = hash.toCharArray()
        val index = Random.nextInt(chars.size)

        // Change one character
        chars[index] = when {
            chars[index].isDigit() -> if (chars[index] == '9') '0' else (chars[index] + 1)
            chars[index].isLowerCase() -> if (chars[index] == 'z') 'a' else (chars[index] + 1)
            chars[index].isUpperCase() -> if (chars[index] == 'Z') 'A' else (chars[index] + 1)
            else -> 'x'
        }

        return String(chars)
    }

    private fun modifyStringContent(content: String): String {
        if (content.isEmpty()) return "TAMPERED"
        return content + "X" // Simple append
    }

    private fun modifySignatureString(signature: String): String {
        return modifyHashString(signature) // Signatures are typically base64 or hex strings
    }

    private fun modifyPublicKeyString(publicKey: String): String {
        return modifyHashString(publicKey) // Public keys are typically encoded strings
    }

    private fun generateRandomHash(): String {
        val chars = "0123456789abcdef"
        return (1..64).map { chars.random() }.joinToString("")
    }
}