package com.ledger.app.services.ledger.implementations

import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Ledger
import com.ledger.app.models.ledger.Page
import com.ledger.app.models.ledger.PageBuilder.Companion.computeMerkleTree
import com.ledger.app.repositories.ledger.LedgerRepo
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB
import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider
import org.springframework.scheduling.annotation.Scheduled

data class LedgerIntegrityReport(
    val ledgerName: String,
    val firstPage: Int,
    val lastPage: Int,
    val result: ValidationResult,
    val context: String
)

enum class ValidationResult {
    VALIDATION_SUCCESSFUL,
    TAMPERING_DETECTED
}

data class ValidationError(
    val type: ErrorType,
    val pageNumber: Int?,
    val entryId: String?,
    val reason: String
)

enum class ErrorType {
    HASH_DISCREPANCY,
    TIMESTAMP_DISCREPANCY,
    SIGNATURE_INVALID,
    MISSING_SIGNATURE,
    LEDGER_NAME_MISMATCH,
    PAGE_NUMBER_MISMATCH,
    MERKLE_ROOT_MISMATCH,
    PREVIOUS_HASH_MISMATCH
}

//@Service
class LedgerWarden(
    private val repo: LedgerRepo
) {
    private val logger = ColorLogger("LedgerWarden", RGB.PINK_LIGHT, LogLevel.DEBUG)

    @Scheduled(fixedDelayString = "\${ledger.warden.interval:30000}") // Default 30 seconds
    fun validateAllLedgers() {
        logger.info("Starting scheduled ledger integrity validation")

        val ledgerConfigs = repo.getAllLedgers()
        val reports = mutableListOf<LedgerIntegrityReport>()

        for (config in ledgerConfigs) {
            try {
                val report = validateLedger(config.name)
                reports.add(report)

                when (report.result) {
                    ValidationResult.VALIDATION_SUCCESSFUL -> logger.info("✓ Ledger '${config.name}': ${report.context}")
                    ValidationResult.TAMPERING_DETECTED -> logger.error("✗ Ledger '${config.name}': ${report.context}")
                }
            } catch (e: Exception) {
                logger.error("Failed to validate ledger '${config.name}': ${e.message}")
                reports.add(
                    LedgerIntegrityReport(
                        ledgerName = config.name,
                        firstPage = -1,
                        lastPage = -1,
                        result = ValidationResult.TAMPERING_DETECTED,
                        context = "Validation Error: ${e.message}"
                    )
                )
            }
        }
    }

    fun validateLedger(ledgerName: String): LedgerIntegrityReport {
        val ledger = repo.readLedger(ledgerName)
            ?: return LedgerIntegrityReport(
                ledgerName = ledgerName,
                firstPage = -1,
                lastPage = -1,
                result = ValidationResult.TAMPERING_DETECTED,
                context = "Ledger not found"
            )

        if (ledger.pages.isEmpty()) {
            return LedgerIntegrityReport(
                ledgerName = ledgerName,
                firstPage = -1,
                lastPage = -1,
                result = ValidationResult.VALIDATION_SUCCESSFUL,
                context = "No pages to validate"
            )
        }

        val firstPage = ledger.pages.minOfOrNull { it.number } ?: 0
        val lastPage = ledger.pages.maxOfOrNull { it.number } ?: -1
        val allPages = ledger.pages.sortedBy { it.number }

        for (page in allPages) {
            val pageValidation = validatePage(page, ledger)
            if (pageValidation != null) {
                return LedgerIntegrityReport(
                    ledgerName = ledgerName,
                    firstPage = firstPage,
                    lastPage = lastPage,
                    result = ValidationResult.TAMPERING_DETECTED,
                    context = "Tampering found at page ${page.number}, where ${pageValidation.reason}"
                )
            }
        }

        return LedgerIntegrityReport(
            ledgerName = ledgerName,
            firstPage = firstPage,
            lastPage = lastPage,
            result = ValidationResult.VALIDATION_SUCCESSFUL,
            context = "No tampering found"
        )
    }

    private fun validatePage(page: Page, ledger: Ledger): ValidationError? {
        // Validate ledger name matches
        if (page.ledgerName != ledger.config.name) {
            return ValidationError(
                type = ErrorType.LEDGER_NAME_MISMATCH,
                pageNumber = page.number,
                entryId = null,
                reason = "page ledger name '${page.ledgerName}' doesn't match ledger '${ledger.config.name}'"
            )
        }

        // Validate page number consistency
        val expectedPageNumber = ledger.pages.filter { it.number < page.number }.size
        if (page.number != expectedPageNumber && page.number != ledger.pages.indexOf(page)) {
            return ValidationError(
                type = ErrorType.PAGE_NUMBER_MISMATCH,
                pageNumber = page.number,
                entryId = null,
                reason = "page number inconsistency"
            )
        }

        // Validate timestamp ordering
        val previousPage = ledger.pages.find { it.number == page.number - 1 }
        val nextPage = ledger.pages.find { it.number == page.number + 1 }

        if (previousPage != null && page.timestamp <= previousPage.timestamp) {
            return ValidationError(
                type = ErrorType.TIMESTAMP_DISCREPANCY,
                pageNumber = page.number,
                entryId = null,
                reason = "page timestamp ${page.timestamp} is not after previous page timestamp ${previousPage.timestamp}"
            )
        }

        if (nextPage != null && page.timestamp >= nextPage.timestamp) {
            return ValidationError(
                type = ErrorType.TIMESTAMP_DISCREPANCY,
                pageNumber = page.number,
                entryId = null,
                reason = "page timestamp ${page.timestamp} is not before next page timestamp ${nextPage.timestamp}"
            )
        }

        // Validate previous hash
        val expectedPreviousHash = previousPage?.hash
        if (page.previousHash != expectedPreviousHash) {
            return ValidationError(
                type = ErrorType.PREVIOUS_HASH_MISMATCH,
                pageNumber = page.number,
                entryId = null,
                reason = "previous hash mismatch - expected: '$expectedPreviousHash', actual: '${page.previousHash}'"
            )
        }

        // Validate merkle root
        val calculatedMerkleRoot = calculateMerkleRoot(page.entries, ledger.config.hashAlgorithm)
        if (page.merkleRoot != calculatedMerkleRoot) {
            return ValidationError(
                type = ErrorType.MERKLE_ROOT_MISMATCH,
                pageNumber = page.number,
                entryId = null,
                reason = "merkle root mismatch - expected: '$calculatedMerkleRoot', actual: '${page.merkleRoot}'"
            )
        }

        val calculatedPageHash = calculatePageHash(page, ledger.config.hashAlgorithm)

        // Validate page hash
        if (page.hash != calculatedPageHash) {
            return ValidationError(
                type = ErrorType.HASH_DISCREPANCY,
                pageNumber = page.number,
                entryId = null,
                reason = "page hash mismatch - expected: '$calculatedPageHash', actual: '${page.hash}'"
            )
        }

        // Validate all entries in the page
        for (entry in page.entries) {
            val entryValidation = validateEntry(entry, page, ledger)
            if (entryValidation != null) {
                return entryValidation
            }
        }

        return null
    }

    // Add this helper method to your LedgerWarden class
    private fun formatTimestamp(timestamp: Long): String {
        return java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            java.time.ZoneId.systemDefault()
        ).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    }

    private fun validateEntry(entry: Entry, page: Page, ledger: Ledger): ValidationError? {
        // Validate ledger name matches
        if (entry.ledgerName != page.ledgerName) {
            return ValidationError(
                type = ErrorType.LEDGER_NAME_MISMATCH,
                pageNumber = page.number,
                entryId = entry.id,
                reason = "entry ledger name '${entry.ledgerName}' doesn't match page ledger name '${page.ledgerName}'"
            )
        }

        // Validate page number matches
        if (entry.pageNum != null && entry.pageNum != page.number) {
            return ValidationError(
                type = ErrorType.PAGE_NUMBER_MISMATCH,
                pageNumber = page.number,
                entryId = entry.id,
                reason = "entry page number ${entry.pageNum} doesn't match actual page number ${page.number}"
            )
        }

        // Validate timestamp is before page timestamp
        if (entry.timestamp >= page.timestamp) {
            return ValidationError(
                type = ErrorType.TIMESTAMP_DISCREPANCY,
                pageNumber = page.number,
                entryId = entry.id,
                reason = "entry timestamp ${entry.timestamp} is not before page timestamp ${page.timestamp}"
            )
        }

        // Validate entry hash
        val calculatedEntryHash = calculateEntryHash(entry, ledger.config.hashAlgorithm)
        if (entry.hash != calculatedEntryHash) {
            return ValidationError(
                type = ErrorType.HASH_DISCREPANCY,
                pageNumber = page.number,
                entryId = entry.id,
                reason = "entry hash mismatch - expected: '$calculatedEntryHash', actual: '${entry.hash}'"
            )
        }

        // Validate signatures
        val signatureValidation = validateEntrySignatures(entry)
        if (signatureValidation != null) {
            return signatureValidation.copy(pageNumber = page.number)
        }

        // TODO: Validate participants exist in database (not implemented yet)
        // TODO: Validate public keys are associated with sender IDs (not implemented yet)

        return null
    }

    private fun validateEntrySignatures(entry: Entry): ValidationError? {
        // Check that each signer matches a sender
        for (signature in entry.signatures) {
            if (!entry.senders.contains(signature.signerId)) {
                return ValidationError(
                    type = ErrorType.SIGNATURE_INVALID,
                    pageNumber = null,
                    entryId = entry.id,
                    reason = "signature from '${signature.signerId}' but they are not a sender"
                )
            }
        }

        // Check that no senders are missing their signature
        for (sender in entry.senders) {
            if (!entry.signatures.any { it.signerId == sender }) {
                return ValidationError(
                    type = ErrorType.MISSING_SIGNATURE,
                    pageNumber = null,
                    entryId = entry.id,
                    reason = "missing signature from sender '$sender'"
                )
            }
        }

        // Validate all signatures are cryptographically valid
        for (signature in entry.signatures) {
            try {
                if (!SignatureProvider.verify(
                        entry.hash,
                        signature.signatureData,
                        signature.publicKey,
                        signature.algorithm
                    )
                ) {
                    return ValidationError(
                        type = ErrorType.SIGNATURE_INVALID,
                        pageNumber = null,
                        entryId = entry.id,
                        reason = "invalid signature from '${signature.signerId}'"
                    )
                }
            } catch (e: Exception) {
                return ValidationError(
                    type = ErrorType.SIGNATURE_INVALID,
                    pageNumber = null,
                    entryId = entry.id,
                    reason = "signature verification failed for '${signature.signerId}': ${e.message}"
                )
            }
        }

        return null
    }

    private fun calculateMerkleRoot(entries: List<Entry>, hashAlgorithm: String): String {
        val merkleTree = computeMerkleTree(entries, hashAlgorithm)
        return merkleTree[merkleTree.size - 1][0]
    }

    private fun calculatePageHash(page: Page, hashAlgorithm: String): String {
        val data = listOf(
            page.ledgerName,
            page.number.toString(),
            page.timestamp,
            page.previousHash,
            page.merkleRoot,
        ).joinToString("|")
        return HashProvider.toHexString(HashProvider.hash(data, hashAlgorithm))
    }

    private fun calculateEntryHash(entry: Entry, hashAlgorithm: String): String {
        val data = listOf(
            entry.id,
            entry.timestamp.toString(),
            entry.content,
            entry.senders.joinToString(","),
            entry.recipients.joinToString(",")
        ).joinToString("|")
        return HashProvider.toHexString(HashProvider.hash(data, hashAlgorithm))
    }
}