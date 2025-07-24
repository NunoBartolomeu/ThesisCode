package com.ledger.app.dtos


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
