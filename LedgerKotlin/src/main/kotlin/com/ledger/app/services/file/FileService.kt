package com.ledger.app.services.file

import com.ledger.app.models.FileMetadata
import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile

interface FileService {
    fun saveFile(uploaderId: String, file: MultipartFile, senders: List<String>, receivers: List<String>): FileMetadata
    fun loadFile(userId: String, fileMetadataId: String): Resource
    fun deleteFile(userId: String, fileMetadataId: String): Boolean

    fun getUserAccessibleFiles(userId: String): List<FileMetadata>
    fun getFileMetadata(fileMetadataId: String): FileMetadata?
    fun hasFileAccess(userId: String, fileMetadataId: String): Boolean
}