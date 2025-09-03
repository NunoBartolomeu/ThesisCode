package com.ledger.app.dtos

data class FileMetadataDto(
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
    val wasDeleted: Boolean
)

data class FileListResponse(
    val files: List<FileInfoDTO>
)

data class FileInfoDTO(
    val id: String,      // Added file ID
    val name: String,
    val size: Long,
    val wasDeleted: Boolean
)
