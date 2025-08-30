package com.ledger.app.controllers

import com.ledger.app.dtos.*
import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Ledger
import com.ledger.app.models.ledger.Page
import com.ledger.app.models.ledger.PageSummary
import com.ledger.app.services.auth.AuthService
import com.ledger.app.services.ledger.LedgerService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/ledger")
class LedgerController(
    private val ledgerService: LedgerService,
    private val authService: AuthService
) {
    private val logger = ColorLogger("LedgerController", RGB.RED_DARK, LogLevel.DEBUG)

    @GetMapping("/available")
    fun getAvailableLedgers(): ResponseEntity<List<String>> {
        logger.debug("Fetching available ledgers")
        val ledgers = ledgerService.getAvailableLedgers()
        logger.info("Found ${ledgers.size} ledgers")
        return ResponseEntity.ok(ledgers)
    }

    @PostMapping("/entry/{entryId}/keywords")
    fun addKeywords(
        @PathVariable entryId: String,
        @RequestBody keywords: List<String>
    ): ResponseEntity<Map<String, String>> {
        logger.info("Adding keywords to entry $entryId: $keywords")
        ledgerService.addKeywords(entryId, keywords)
        logger.debug("Keywords added to entry $entryId")
        return ResponseEntity.ok(mapOf("message" to "Keywords added"))
    }

    @DeleteMapping("/entry/{entryId}/keyword/{keyword}")
    fun removeKeyword(
        @PathVariable entryId: String,
        @PathVariable keyword: String
    ): ResponseEntity<Map<String, String>> {
        logger.info("Removing keyword '$keyword' from entry $entryId")
        ledgerService.removeKeyword(entryId, keyword)
        logger.debug("Keyword removed from entry $entryId")
        return ResponseEntity.ok(mapOf("message" to "Keyword removed"))
    }

    @GetMapping("/{ledgerName}")
    fun getLedger(
        @PathVariable ledgerName: String,
        authentication: Authentication
    ): ResponseEntity<LedgerDTO> {
        val userId = getUserId(authentication.principal)
        logger.info("Fetching ledger: $ledgerName for user $userId")
        val ledger = ledgerService.getLedger(ledgerName)
        if (ledger == null) {
            logger.warn("Ledger $ledgerName not found")
            return ResponseEntity.notFound().build()
        }
        logger.debug("Ledger $ledgerName fetched successfully")
        return ResponseEntity.ok(ledger.toLedgerDTO(userId))
    }

    @GetMapping("/{ledgerName}/page/{pageNumber}")
    fun getPageSummary(
        @PathVariable ledgerName: String,
        @PathVariable pageNumber: Int,
        authentication: Authentication
    ): ResponseEntity<PageDTO> {
        val userId = getUserId(authentication.principal)
        logger.info("Fetching page $pageNumber of ledger $ledgerName for user $userId")
        val page = ledgerService.getPage(ledgerName, pageNumber)
        if (page == null) {
            logger.warn("Page $pageNumber of ledger $ledgerName not found")
            return ResponseEntity.notFound().build()
        }
        logger.debug("Page $pageNumber of ledger $ledgerName fetched successfully")
        return ResponseEntity.ok(page.toPageDTO(userId))
    }

    @GetMapping("/entry/{entryId}")
    fun getEntry(
        @PathVariable entryId: String,
        authentication: Authentication
    ): ResponseEntity<EntryDTO> {
        val userId = getUserId(authentication.principal)
        logger.info("Fetching entry $entryId for user $userId")
        val entry = ledgerService.getEntry(entryId)
        if (entry == null) {
            logger.warn("Entry $entryId not found")
            return ResponseEntity.notFound().build()
        }
        logger.debug("Entry $entryId fetched successfully")
        return ResponseEntity.ok(entry.toDTO(userId))
    }

    private fun getUserId(user: Any): String {
        return try {
            when {
                user.javaClass.getDeclaredField("id") != null -> {
                    val field = user.javaClass.getDeclaredField("id")
                    field.isAccessible = true
                    field.get(user).toString()
                }
                user.javaClass.getDeclaredField("userId") != null -> {
                    val field = user.javaClass.getDeclaredField("userId")
                    field.isAccessible = true
                    field.get(user).toString()
                }
                else -> user.toString()
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract user ID: ${e.message}")
            user.toString()
        }
    }

    private fun Entry.toDTO(userId: String): EntryDTO {
        val isParticipant = senders.contains(userId) || recipients.contains(userId)
        return EntryDTO(
            id = id,
            timestamp = timestamp,
            content = if (isParticipant) content else "Not available for user",
            senders = senders.map { resolveParticipant(it) },
            recipients = recipients.map { resolveParticipant(it) },
            signatures = signatures.map {
                SignatureDTO(
                    participant = resolveName(it.signerId),
                    email = it.signerId,
                    publicKey = it.publicKey,
                    signature = it.signatureData
                )
            },
            relatedEntryIds = relatedEntries,
            keywords = keywords,
            ledgerName = ledgerName,
            pageNumber = pageNum,
            hash = hash
        )
    }

    private fun resolveParticipant(id: String): ParticipantDTO {
        return ParticipantDTO(
            fullName = resolveName(id),
            email = id
        )
    }

    private fun resolveName(id: String): String {
        val info = authService.getUserInfo(id)?: throw IllegalArgumentException("User not found")
        return info.fullName
    }

    private fun Ledger.toLedgerDTO(userId: String): LedgerDTO {
        logger.debug("Converting Ledger model to DTO for user $userId")
        return LedgerDTO(
            name = config.name,
            entriesPerPage = config.entriesPerPage,
            hashAlgorithm = config.hashAlgorithm,
            verifiedEntries = verifiedEntries.map {it.toPageEntryDTO(userId) },
            unverifiedEntries = holdingArea.map { it.toPageEntryDTO(userId) },
            pages = pages.map { it.toPageSummary().toDTO() }
        )
    }

    private fun PageSummary.toDTO(): PageSummaryDTO {
        return PageSummaryDTO(
            number = number,
            timestamp = timestamp,
            entryCount = entryCount
        )
    }

    private fun Entry.toPageEntryDTO(userId: String): PageEntryDTO {
        val isParticipant = senders.contains(userId) || recipients.contains(userId)
        return PageEntryDTO(
            id = id,
            isParticipant = isParticipant
        )
    }

    private fun Page.toPageDTO(userId: String): PageDTO {
        return PageDTO(
            ledgerName = ledgerName,
            number = number,
            timestamp = timestamp,
            previousHash = previousHash,
            entryCount = entries.count(),
            hash = hash,
            entries = entries.map { it.toPageEntryDTO(userId) }
        )
    }
}
