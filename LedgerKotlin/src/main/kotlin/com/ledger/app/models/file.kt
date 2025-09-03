package com.ledger.app.models
data class FileMetadata(
    val id: String,
    val originalFileName: String,
    val actualFileName: String,
    val filePath: String,
    val fileSize: Long,
    val contentType: String?,
    val uploadedAt: Long,

    val uploaderId: String,
    val senders: List<String>,
    val receivers: List<String>,
    val ledgerEntries: List<String>,
    val wasDeleted: Boolean = false
)

enum class ParticipantRole {
    UPLOADER,
    SENDER,
    RECEIVER
}

data class FileParticipant(
    val userId: String,
    val fileMetadataId: String,
    val role: ParticipantRole
)