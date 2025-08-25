package com.ledger.app.repositories.files.implementations

import com.ledger.app.models.FileMetadata
import com.ledger.app.repositories.files.FilesRepo
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Repository
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

@Repository
class FilesRepoMemory : FilesRepo {

    // In-memory storage: userId -> fileName -> FileMetadata
    private val filesMetadata = ConcurrentHashMap<String, ConcurrentHashMap<String, FileMetadata>>()
    private val uploadDirectory = "uploads" // You can make this configurable

    override fun saveFile(userId: String, file: MultipartFile): FileMetadata {
        try {
            val userDir = getUserDirectory(userId)

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

            // Create and save metadata
            val metadata = FileMetadata(
                userId = userId,
                originalFileName = originalFilename,
                actualFileName = finalFile.name,
                filePath = finalFile.absolutePath,
                fileSize = finalFile.length(),
                contentType = file.contentType,
                uploadedAt = System.currentTimeMillis(),
                lastAccessed = null
            )

            // Save metadata to in-memory storage
            val userFiles = filesMetadata.computeIfAbsent(userId) { ConcurrentHashMap() }
            userFiles[originalFilename] = metadata

            return metadata

        } catch (e: IOException) {
            throw RuntimeException("Could not save file: ${e.message}", e)
        } catch (e: Exception) {
            throw RuntimeException("Could not save file: ${e.message}", e)
        }
    }

    override fun loadFile(userId: String, fileName: String): Resource {
        try {
            // Check metadata first
            val metadata = getFileMetadata(userId, fileName)
                ?: throw RuntimeException("File not found")

            val file = File(metadata.filePath)

            // Security check: ensure the resolved path is within the user directory
            val userDir = getUserDirectory(userId)
            val userDirPath = userDir.toPath().toRealPath()
            val filePath = file.toPath().toRealPath()

            if (!filePath.startsWith(userDirPath)) {
                throw SecurityException("Access denied: File path outside user directory")
            }

            if (!file.exists()) {
                throw RuntimeException("File not found on disk")
            }

            // Update last accessed timestamp
            updateLastAccessed(userId, fileName)

            return FileSystemResource(file)

        } catch (e: IOException) {
            throw RuntimeException("Could not load file: ${e.message}", e)
        } catch (e: SecurityException) {
            throw RuntimeException("Access denied", e)
        }
    }

    override fun deleteFile(userId: String, fileName: String): Boolean {
        return try {
            // Get metadata first
            val metadata = getFileMetadata(userId, fileName) ?: return false

            val file = File(metadata.filePath)

            // Security check: ensure the resolved path is within the user directory
            val userDir = getUserDirectory(userId)
            val userDirPath = userDir.toPath().toRealPath()
            val filePath = file.toPath().toRealPath()

            if (!filePath.startsWith(userDirPath)) {
                throw SecurityException("Access denied: File path outside user directory")
            }

            // Delete physical file
            val physicalFileDeleted = if (file.exists()) {
                file.delete()
            } else {
                true // Consider it "deleted" if it doesn't exist
            }

            physicalFileDeleted

        } catch (e: Exception) {
            false
        }
    }

    override fun listUserFiles(userId: String): List<FileMetadata> {
        val metadataList = getUserFileMetadata(userId)

        return metadataList.filter { metadata ->
            val file = File(metadata.filePath)
            if (file.exists() && file.isFile) {
                true
            } else {
                false
            }
        }
    }

    override fun getFileMetadata(userId: String, fileName: String): FileMetadata? {
        val userFiles = filesMetadata[userId] ?: return null
        return userFiles[fileName]
    }

    override fun getUserFileMetadata(userId: String): List<FileMetadata> {
        val userFiles = filesMetadata[userId] ?: return emptyList()
        return userFiles.values.toList()
    }

    override fun fileExists(userId: String, fileName: String): Boolean {
        val userFiles = filesMetadata[userId] ?: return false
        return userFiles.containsKey(fileName)
    }

    override fun updateLastAccessed(userId: String, fileName: String): Boolean {
        return try {
            val userFiles = filesMetadata[userId] ?: return false
            val metadata = userFiles[fileName] ?: return false

            val updatedMetadata = metadata.copy(lastAccessed = System.currentTimeMillis())
            userFiles[fileName] = updatedMetadata
            true
        } catch (e: Exception) {
            false
        }
    }

    // Utility methods
    private fun getUserDirectory(userId: String): File {
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

    // Additional utility methods
    fun getUserStorageSize(userId: String): Long {
        val userFiles = filesMetadata[userId] ?: return 0L
        return userFiles.values.sumOf { it.fileSize }
    }

    fun getUserFileCount(userId: String): Int {
        val userFiles = filesMetadata[userId] ?: return 0
        return userFiles.size
    }

    fun getAllUsersWithFiles(): Set<String> {
        return filesMetadata.keys.toSet()
    }
}