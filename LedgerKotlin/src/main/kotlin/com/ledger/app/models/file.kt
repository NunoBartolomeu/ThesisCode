package com.ledger.app.models

data class FileMetadata(
    val userId: String,
    val originalFileName: String,
    val actualFileName: String,
    val filePath: String,
    val fileSize: Long,
    val contentType: String?,
    val uploadedAt: Long,
    val lastAccessed: Long?
)