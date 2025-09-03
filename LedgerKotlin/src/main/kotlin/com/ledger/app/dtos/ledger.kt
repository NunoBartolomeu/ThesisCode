package com.ledger.app.dtos

import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Ledger
import com.ledger.app.models.ledger.Page
import com.ledger.app.services.auth.AuthService
import java.util.UUID

data class LedgerDTO(
    val name: String,
    val entriesPerPage: Int,
    val hashAlgorithm: String,
    val pages: List<PageSummaryDTO>,
    val unverifiedEntries: List<PageEntryDTO>,
    val verifiedEntries: List<PageEntryDTO>,
)

data class PageSummaryDTO(
    val number: Int,
    val timestamp: Long,
    val entryCount: Int
)

data class PageDTO(
    val ledgerName: String,
    val number: Int,
    val timestamp: Long,
    val previousHash: String?,
    val merkleRoot: String,
    val hash: String,
    val entryCount: Int,
    val entries: List<PageEntryDTO>
)

data class PageEntryDTO(
    val id: String,
    val isParticipant: Boolean
)

data class EntryDTO(
    val id: String,
    val timestamp: Long,
    val content: String,
    val senders: List<ParticipantDTO>,
    val recipients: List<ParticipantDTO>,
    val hash: String,
    val signatures: List<SignatureDTO>,
    val ledgerName: String,
    val pageNumber: Int?,
    val relatedEntryIds: List<String>,
    val keywords: List<String>,
)

data class ParticipantDTO(
    val fullName: String,
    val email: String
)

data class SignatureDTO(
    val participant: String, // fullName
    val email: String,
    val publicKey: String,
    val signature: String,
    val algorithm: String
)

// Extension functions for DTO conversions

fun Ledger.toLedgerDTO(userId: String): LedgerDTO {
    return LedgerDTO(
        name = config.name,
        entriesPerPage = config.entriesPerPage,
        hashAlgorithm = config.hashAlgorithm,
        verifiedEntries = verifiedEntries.map { it.toPageEntryDTO(userId) },
        unverifiedEntries = holdingArea.map { it.toPageEntryDTO(userId) },
        pages = pages.map { it.toPageSummaryDTO() }
    )
}

fun Page.toPageSummaryDTO(): PageSummaryDTO {
    return PageSummaryDTO(
        number = number,
        timestamp = timestamp,
        entryCount = entries.count()
    )
}

fun Page.toPageDTO(userId: String): PageDTO {
    return PageDTO(
        ledgerName = ledgerName,
        number = number,
        timestamp = timestamp,
        previousHash = previousHash,
        entryCount = entries.count(),
        hash = hash,
        merkleRoot = merkleRoot,
        entries = entries.map { it.toPageEntryDTO(userId) }
    )
}

fun Entry.toPageEntryDTO(userId: String): PageEntryDTO {
    val isParticipant = senders.contains(userId) || recipients.contains(userId)
    return PageEntryDTO(
        id = id,
        isParticipant = isParticipant
    )
}

fun Entry.toEntryDTO(userId: String, authService: AuthService): EntryDTO {
    val isParticipant = senders.contains(userId) || recipients.contains(userId)
    return EntryDTO(
        id = id,
        timestamp = timestamp,
        content = if (isParticipant) content else "Not available for user",
        senders = senders.map { it.resolveParticipant(authService) },
        recipients = recipients.map { it.resolveParticipant(authService) },
        signatures = signatures.map {
            val signer = it.signerId.resolveParticipant(authService)
            SignatureDTO(
                participant = signer.fullName,
                email = signer.email,
                publicKey = it.publicKey,
                signature = it.signatureData,
                algorithm = it.algorithm
            )
        },
        relatedEntryIds = relatedEntries,
        keywords = keywords,
        ledgerName = ledgerName,
        pageNumber = pageNum,
        hash = hash
    )
}

private fun String.resolveParticipant(authService: AuthService): ParticipantDTO {
    return if (this.length == UUID.randomUUID().toString().length) {
        val user = authService.getUserInfo(this)
            ?: throw IllegalArgumentException("User not found")
        ParticipantDTO(
            fullName = user.fullName,
            email = user.email
        )
    } else {
        ParticipantDTO(
            fullName = this,
            email = "Email not available for system entries."
        )
    }
}