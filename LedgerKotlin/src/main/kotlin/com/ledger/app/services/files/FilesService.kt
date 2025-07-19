package com.ledger.app.services.files

import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile
import java.io.File

interface FilesService {
    fun saveFile(userId: String, file: MultipartFile): File
    fun loadFile(userId: String, fileName: String): Resource
    fun listUserFiles(userId: String): List<File>
    fun deleteFile(userId: String, fileName: String): Boolean
    fun getUserDirectory(userId: String): File
}