package com.ledger.app.repositories.file

import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile
import java.io.File

interface FileRepo {
    fun saveFile(uploaderId: String, file: MultipartFile): File
    fun loadFile(userId: String, fileName: String): Resource
    fun deleteFile(userId: String, fileName: String): Boolean
    fun fileExists(userId: String, fileName: String): Boolean
    fun getFileSize(userId: String, fileName: String): Long?
}