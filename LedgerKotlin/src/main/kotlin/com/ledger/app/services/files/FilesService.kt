package com.ledger.app.services.files

import com.ledger.app.dtos.FileDetailsDto
import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile
import java.io.File

interface FilesService {
    fun saveFile(userId: String, file: MultipartFile): File
    fun loadFile(userId: String, fileName: String): Resource
    fun deleteFile(userId: String, fileName: String): Boolean
    fun listUserFiles(userId: String): List<File>
    fun getUserDirectory(userId: String): File
    fun getFileDetails(userId: String, fileName: String): FileDetailsDto?
    fun initiateLedgerForUser(userId: String)

    //TODO add  encryption and decryption for files
}