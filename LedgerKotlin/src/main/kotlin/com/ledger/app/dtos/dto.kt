package com.ledger.app.dtos

import java.time.LocalDateTime

data class LoginRequest(val email: String, val passwordHash: ByteArray)
data class RegisterRequest(val email: String, val passwordHash: ByteArray, val fullName: String)
data class VerifyCodeRequest(val email: String, val code: String)
data class ValidateTokenRequest(val token: String)
data class FileDetailsDto(val originalFileName: String, val actualFileName: String, val fileSize: Long, val contentType: String?, val uploadedAt: Long, val lastAccessed: Long?, val ownerFullName: String?, val ownerEmail: String?)



data class LedgerDTO(
    val name: String,
    val entriesPerPage: Int,
    val hashAlgorithm: String,
    val cryptoAlgorithm: String,
    val holdingEntries: List<PageEntryDTO>,
    val pages: List<PageSummaryDTO>
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
    val entryCount: Int,
    val hash: String,
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
    val signatures: List<SignatureDTO>,
    val relatedEntryIds: List<String>,
    val keywords: List<String>,
    val ledgerName: String,
    val pageNumber: Int?, // null if in holding
    val hash: String
)

data class ParticipantDTO(
    val fullName: String,
    val email: String
)

data class SignatureDTO(
    val participant: String, // fullName
    val email: String,
    val publicKey: String,
    val signature: String
)
