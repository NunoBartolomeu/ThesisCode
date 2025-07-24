package com.ledger.app.dtos

data class FileDetailsDto(
    val originalFileName: String,
    val actualFileName: String,
    val fileSize: Long,
    val contentType: String?,
    val uploadedAt: Long,
    val lastAccessed: Long?,
    val ownerFullName: String?,
    val ownerEmail: String?
)

data class FileListResponse(
    val files: List<FileInfoDTO>
)

data class FileInfoDTO(
    val name: String,
    val size: Long,
    val lastModified: Long
)

