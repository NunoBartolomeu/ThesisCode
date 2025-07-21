package com.ledger.app.controllers

import com.ledger.app.dtos.*
import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Ledger
import com.ledger.app.models.ledger.Page
import com.ledger.app.models.ledger.PageSummary
import com.ledger.app.services.ledger.LedgerService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.HashProvider
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/ledger")
class LedgerController(
    private val ledgerService: LedgerService
) {
    private val logger = ColorLogger("LedgerController", RGB.RED_DARK, LogLevel.DEBUG)

    @GetMapping("/available")
    fun getAvailableLedgers(): ResponseEntity<List<String>> {
        val ledgers = ledgerService.getAvailableLedgers()
        return ResponseEntity.ok(ledgers)
    }

    @PostMapping("/entry/{entryId}/keywords")
    fun addKeywords(
        @PathVariable entryId: String,
        @RequestBody keywords: List<String>
    ): ResponseEntity<Map<String, String>> {
        ledgerService.addKeywords(entryId, keywords)
        return ResponseEntity.ok(mapOf("message" to "Keywords added"))
    }

    @DeleteMapping("/entry/{entryId}/keyword/{keyword}")
    fun removeKeyword(
        @PathVariable entryId: String,
        @PathVariable keyword: String
    ): ResponseEntity<Map<String, String>> {
        ledgerService.removeKeyword(entryId, keyword)
        return ResponseEntity.ok(mapOf("message" to "Keyword removed"))
    }

    @GetMapping("/{ledgerName}")
    fun getLedger(
        @PathVariable ledgerName: String,
        authentication: Authentication
    ): ResponseEntity<LedgerDTO> {
        val userId = getUserId(authentication.principal)
        val ledger = ledgerService.getLedgerWithPages(ledgerName) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ledger.toLedgerDTO(userId))
    }

    @GetMapping("/{ledgerName}/page/{pageNumber}")
    fun getPageSummary(
        @PathVariable ledgerName: String,
        @PathVariable pageNumber: Int,
        authentication: Authentication
    ): ResponseEntity<PageDTO> {
        val userId = getUserId(authentication.principal)
        val page = ledgerService.getPage(ledgerName, pageNumber)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(page.toPageDTO(userId))
    }

    @GetMapping("/entry/{entryId}")
    fun getEntry(
        @PathVariable entryId: String,
        authentication: Authentication
    ): ResponseEntity<EntryDTO> {
        val userId = getUserId(authentication.principal)
        val entry = ledgerService.getEntry(entryId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(entry.toDTO(userId))
    }

    private fun getUserId(user: Any): String {
        return when {
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
    }

    private fun Entry.toDTO(userId: String): EntryDTO {
        val isParticipant = senders.contains(userId) || recipients.contains(userId)
        return EntryDTO(
            id = id,
            timestamp = timestamp,
            content = if (isParticipant) content.toString(Charsets.UTF_8) else "Not available for user",
            senders = senders.map { resolveParticipant(it) },
            recipients = recipients.map { resolveParticipant(it) },
            signatures = signatures.map {
                SignatureDTO(
                    participant = resolveName(it.signerId),
                    email = it.signerId,
                    publicKey = it.publicKey.toHex(),
                    signature = it.signature.toHex()
                )
            },
            relatedEntryIds = relatedEntries,
            keywords = keywords,
            ledgerName = ledgerName,
            pageNumber = pageNum,
            hash = computeHash { ledgerService.getHasher().hash(it) }.toHex()
        )
    }

    private fun resolveParticipant(id: String): ParticipantDTO {
        // Placeholder: replace with user service lookup if needed
        return ParticipantDTO(
            fullName = resolveName(id),
            email = id
        )
    }

    private fun resolveName(id: String): String {
        return if (id.lowercase().contains("system")) id else "User $id"
    }

    private fun Ledger.toLedgerDTO(userId: String): LedgerDTO {
        val dto = LedgerDTO(
            name = config.name,
            entriesPerPage = config.entriesPerPage,
            hashAlgorithm = config.hashAlgorithm,
            cryptoAlgorithm = config.cryptoAlgorithm,
            holdingEntries = holdingArea.map { it.toPageEntryDTO(userId) },
            pages = pages.map { it.toPageSummary().toDTO() }
        )
        println("Here in the ledgerDTO")
        println("The ledger is: \n$this")
        println("The dto is: \n$dto")
        return dto
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
            this.ledgerName,
            this.number,
            this.timestamp,
            this.previousHash?.toHex(),
            this.entries.count(),
            this.computeHash { ledgerService.getHasher().hash(it) }.toHex(),
            this.entries.map { it.toPageEntryDTO(userId) }
        )
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }
}
