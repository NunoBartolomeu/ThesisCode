package com.ledger.app.services.files

import com.ledger.app.dtos.FileDetailsDto
import com.ledger.app.models.FileMetadata
import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile
import java.io.File

interface FilesRepo {
    fun saveFile(userId: String, file: MultipartFile): FileMetadata
    fun loadFile(userId: String, fileName: String): Resource
    fun deleteFile(userId: String, fileName: String): Boolean
    fun listUserFiles(userId: String): List<FileMetadata>

    fun getFileMetadata(userId: String, fileName: String): FileMetadata?
    fun getUserFileMetadata(userId: String): List<FileMetadata>
    fun fileExists(userId: String, fileName: String): Boolean
    fun updateLastAccessed(userId: String, fileName: String): Boolean
}