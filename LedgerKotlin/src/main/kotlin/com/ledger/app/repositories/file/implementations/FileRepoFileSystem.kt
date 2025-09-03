package com.ledger.app.repositories.file.implementations

import com.ledger.app.repositories.file.FileRepo
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Repository
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Repository
class FileRepoFileSystem : FileRepo {

    @Value("\${app.files.uploadDirectory:uploads}")
    private lateinit var uploadDirectory: String

    override fun saveFile(uploaderId: String, file: MultipartFile): File {
        try {
            val userDir = getUserDirectory(uploaderId)

            // Ensure user directory exists
            if (!userDir.exists()) {
                userDir.mkdirs()
            }

            // Generate safe filename (remove any path traversal attempts)
            val originalFilename = file.originalFilename ?: "unknown_file"
            val safeFilename = sanitizeFilename(originalFilename)

            val targetFile = File(userDir, safeFilename)

            // Handle file name conflicts by appending a number
            val finalFile = resolveFileNameConflict(targetFile)

            // Save the physical file
            file.inputStream.use { inputStream ->
                Files.copy(inputStream, finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            return finalFile

        } catch (e: IOException) {
            throw RuntimeException("Could not save file: ${e.message}", e)
        } catch (e: Exception) {
            throw RuntimeException("Could not save file: ${e.message}", e)
        }
    }

    override fun loadFile(userId: String, fileName: String): Resource {
        try {
            val userDir = getUserDirectory(userId)
            val file = File(userDir, fileName)

            // Security check: ensure the resolved path is within the user directory
            val userDirPath = userDir.toPath().toRealPath()
            val filePath = file.toPath().toRealPath()

            if (!filePath.startsWith(userDirPath)) {
                throw SecurityException("Access denied: File path outside user directory")
            }

            if (!file.exists()) {
                throw RuntimeException("File not found on disk")
            }

            return FileSystemResource(file)

        } catch (e: IOException) {
            throw RuntimeException("Could not load file: ${e.message}", e)
        } catch (e: SecurityException) {
            throw RuntimeException("Access denied", e)
        }
    }

    override fun deleteFile(userId: String, fileName: String): Boolean {
        return try {
            val userDir = getUserDirectory(userId)
            val file = File(userDir, fileName)

            // Security check: ensure the resolved path is within the user directory
            val userDirPath = userDir.toPath().toRealPath()
            val filePath = file.toPath().toRealPath()

            if (!filePath.startsWith(userDirPath)) {
                throw SecurityException("Access denied: File path outside user directory")
            }

            // Delete physical file
            if (file.exists()) {
                file.delete()
            } else {
                true // Consider it "deleted" if it doesn't exist
            }

        } catch (e: Exception) {
            false
        }
    }

    override fun fileExists(userId: String, fileName: String): Boolean {
        return try {
            val userDir = getUserDirectory(userId)
            val file = File(userDir, fileName)

            // Security check: ensure the resolved path is within the user directory
            val userDirPath = userDir.toPath().toRealPath()
            val filePath = file.toPath().toRealPath()

            if (!filePath.startsWith(userDirPath)) {
                return false
            }

            file.exists() && file.isFile
        } catch (e: Exception) {
            false
        }
    }

    override fun getFileSize(userId: String, fileName: String): Long? {
        return try {
            val userDir = getUserDirectory(userId)
            val file = File(userDir, fileName)

            // Security check: ensure the resolved path is within the user directory
            val userDirPath = userDir.toPath().toRealPath()
            val filePath = file.toPath().toRealPath()

            if (!filePath.startsWith(userDirPath)) {
                return null
            }

            if (file.exists() && file.isFile) {
                file.length()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Utility methods
    private fun getUserDirectory(userId: String): File {
        //TODO IS THIS REALLY AN IMPORTANT FUNCTION

        // Sanitize userId to prevent directory traversal
        val safeUserId = sanitizeFilename(userId)
        return File(uploadDirectory, safeUserId)
    }

    private fun sanitizeFilename(filename: String): String {
        // Remove any path separators and other potentially dangerous characters
        return filename
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(255) // Limit filename length
    }

    private fun resolveFileNameConflict(file: File): File {
        if (!file.exists()) {
            return file
        }

        val nameWithoutExtension = file.nameWithoutExtension
        val extension = file.extension
        val parentDir = file.parentFile

        var counter = 1
        var newFile: File

        do {
            val newName = if (extension.isNotEmpty()) {
                "${nameWithoutExtension}_$counter.$extension"
            } else {
                "${nameWithoutExtension}_$counter"
            }
            newFile = File(parentDir, newName)
            counter++
        } while (newFile.exists())

        return newFile
    }
}