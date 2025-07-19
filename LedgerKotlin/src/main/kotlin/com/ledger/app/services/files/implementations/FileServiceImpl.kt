package com.ledger.app.services.files.implementations

import com.ledger.app.services.files.FilesRepo
import com.ledger.app.services.files.FilesService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.Rgb
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Service
class FilesServiceImpl(
    //private val filesRepo: FilesRepo
) : FilesService {
    private val logger = ColorLogger("FilesService", Rgb(100, 255, 100), LogLevel.DEBUG)
    private val uploadDirectory = "uploads" // You can make this configurable

    override fun saveFile(userId: String, file: MultipartFile): File {
        try {
            val userDir = getUserDirectory(userId)

            // Ensure user directory exists
            if (!userDir.exists()) {
                userDir.mkdirs()
                logger.info("Created directory for user $userId: ${userDir.absolutePath}")
            }

            // Generate safe filename (remove any path traversal attempts)
            val originalFilename = file.originalFilename ?: "unknown_file"
            val safeFilename = sanitizeFilename(originalFilename)

            val targetFile = File(userDir, safeFilename)

            // Handle file name conflicts by appending a number
            val finalFile = resolveFileNameConflict(targetFile)

            // Save the file
            file.inputStream.use { inputStream ->
                Files.copy(inputStream, finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            logger.debug("File saved successfully: ${finalFile.absolutePath}")
            return finalFile

        } catch (e: IOException) {
            logger.error("IOException while saving file: ${e.message}")
            throw RuntimeException("Could not save file: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error while saving file: ${e.message}")
            throw RuntimeException("Could not save file: ${e.message}", e)
        }
    }

    override fun loadFile(userId: String, fileName: String): Resource {
        try {
            val userDir = getUserDirectory(userId)
            val safeFilename = sanitizeFilename(fileName)
            val file = File(userDir, safeFilename)

            // Security check: ensure the resolved path is within the user directory
            val userDirPath = userDir.toPath().toRealPath()
            val filePath = file.toPath().toRealPath()

            if (!filePath.startsWith(userDirPath)) {
                throw SecurityException("Access denied: File path outside user directory")
            }

            return FileSystemResource(file)

        } catch (e: IOException) {
            logger.error("IOException while loading file: ${e.message}")
            throw RuntimeException("Could not load file: ${e.message}", e)
        }
    }

    override fun listUserFiles(userId: String): List<File> {
        val userDir = getUserDirectory(userId)

        if (!userDir.exists()) {
            logger.debug("User directory does not exist for user $userId")
            return emptyList()
        }

        return userDir.listFiles()?.filter { it.isFile } ?: emptyList()
    }

    override fun deleteFile(userId: String, fileName: String): Boolean {
        return try {
            val userDir = getUserDirectory(userId)
            val safeFilename = sanitizeFilename(fileName)
            val file = File(userDir, safeFilename)

            // Security check: ensure the resolved path is within the user directory
            val userDirPath = userDir.toPath().toRealPath()
            val filePath = file.toPath().toRealPath()

            if (!filePath.startsWith(userDirPath)) {
                throw SecurityException("Access denied: File path outside user directory")
            }

            if (file.exists() && file.delete()) {
                logger.debug("File deleted successfully: ${file.absolutePath}")
                true
            } else {
                logger.warn("Failed to delete file or file does not exist: ${file.absolutePath}")
                false
            }
        } catch (e: Exception) {
            logger.error("Error deleting file: ${e.message}")
            false
        }
    }

    override fun getUserDirectory(userId: String): File {
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