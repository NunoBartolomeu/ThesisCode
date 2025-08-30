package com.ledger.app.services.files.implementations

import com.ledger.app.dtos.FileDetailsDto
import com.ledger.app.models.FileMetadata
import com.ledger.app.services.auth.AuthService
import com.ledger.app.repositories.files.FilesRepo
import com.ledger.app.services.files.FilesService
import com.ledger.app.services.ledger.LedgerService
import com.ledger.app.utils.*
import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File

@Service
class FilesServiceSpring(
    private val repo: FilesRepo,
    private val ledgerService: LedgerService,
    private val authService: AuthService
) : FilesService {
    private val FILES_SYSTEM = "files_service"
    private val FILES_LEDGER = "files_ledger"
    private val logger = ColorLogger("FilesService", RGB.BLUE_SKY, LogLevel.DEBUG)

    init {
        if (ledgerService.getLedger(FILES_LEDGER) == null) {
            ledgerService.createLedger(FILES_LEDGER, 2, HashProvider.getDefaultAlgorithm())
        }
    }

    override fun initiateLedgerForUser(userId: String) {
        ledgerService.createLedger("user_files_$userId", 10, HashProvider.getDefaultAlgorithm())
    }

    fun writeInUserLedger(userId: String, metadata: FileMetadata, message: String) {
        ledgerService.createEntry(
            ledgerName = "user_files_$userId",
            content = "$message. Metadata $metadata",
            senders = listOf(userId),
            recipients = listOf(),
            relatedEntries = listOf(),
            keywords = listOf()
        )
    }

    override fun saveFile(userId: String, file: MultipartFile): File {
        return try {
            logger.debug("Saving file ${file.originalFilename} for user $userId")
            val metadata = repo.saveFile(userId, file)
            logger.debug("File saved successfully: ${metadata.filePath}")

            writeInUserLedger(userId, metadata,"User $userId uploaded a file")
            ledgerService.logSystemEvent(FILES_LEDGER, FILES_SYSTEM, userId, "File uploaded successfully: ${metadata.originalFileName} (${metadata.fileSize} bytes)")
            File(metadata.filePath)
        } catch (e: Exception) {
            logger.error("Error while saving file: ${e.message}")
            throw RuntimeException("Could not save file: ${e.message}", e)
        }
    }

    override fun loadFile(userId: String, fileName: String): Resource {
        return try {
            logger.debug("Loading file $fileName for user $userId")
            val resource = repo.loadFile(userId, fileName)
            val fileSize = try { resource.contentLength() } catch (e: Exception) { 0L }

            val metadata = repo.getFileMetadata(userId, fileName)
            writeInUserLedger(userId, metadata!!,"User $userId downloaded a file")
            ledgerService.logSystemEvent(FILES_LEDGER, FILES_SYSTEM, userId, "File downloaded successfully: $fileName ($fileSize bytes)")
            resource
        } catch (e: SecurityException) {
            logger.error("Security violation while loading file: ${e.message}")
            throw RuntimeException("Access denied", e)
        } catch (e: Exception) {
            logger.error("Error while loading file: ${e.message}")
            throw RuntimeException("Could not load file: ${e.message}", e)
        }
    }

    override fun listUserFiles(userId: String): List<File> {
        return try {
            logger.debug("Listing files for user $userId")
            val metadataList = repo.listUserFiles(userId)
            val files = metadataList.map { File(it.filePath) }
            logger.debug("Found ${files.size} files for user $userId")
            files
        } catch (e: Exception) {
            logger.error("Error while listing user files: ${e.message}")
            emptyList()
        }
    }

    override fun getFileDetails(userId: String, fileName: String): FileDetailsDto? {
        return try {
            logger.debug("Getting file details for $fileName for user $userId")
            val metadata = repo.getFileMetadata(userId, fileName)
            if (metadata == null) {
                logger.warn("File metadata not found: $fileName for user $userId")
                return null
            }
            val userInfo = authService.getUserInfo(userId)

            FileDetailsDto(
                originalFileName = metadata.originalFileName,
                actualFileName = metadata.actualFileName,
                fileSize = metadata.fileSize,
                contentType = metadata.contentType,
                uploadedAt = metadata.uploadedAt,
                lastAccessed = metadata.lastAccessed,
                ownerFullName = userInfo?.fullName,
                ownerEmail = userInfo?.email
            )

        } catch (e: Exception) {
            logger.error("Error getting file details: ${e.message}")
            null
        }
    }

    override fun deleteFile(userId: String, fileName: String): Boolean {
        return try {
            logger.debug("Deleting file $fileName for user $userId")
            val success = repo.deleteFile(userId, fileName)

            if (success) {
                logger.debug("File deleted successfully: $fileName")
                ledgerService.logSystemEvent(FILES_LEDGER, FILES_SYSTEM, userId, "File deleted successfully: $fileName")
                val metadata = repo.getFileMetadata(userId, fileName)
                writeInUserLedger(userId, metadata!!,"User $userId deleted a file")
            } else {
                logger.warn("Failed to delete file: $fileName")
            }
            success
        } catch (e: Exception) {
            logger.error("Error deleting file: ${e.message}")
            false
        }
    }

    override fun getUserDirectory(userId: String): File {
        val uploadDirectory = "uploads"
        val safeUserId = userId.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(255)
        return File(uploadDirectory, safeUserId)
    }
}