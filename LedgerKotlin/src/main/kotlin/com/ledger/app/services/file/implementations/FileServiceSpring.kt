package com.ledger.app.services.file.implementations

import com.ledger.app.models.FileMetadata
import com.ledger.app.models.FileParticipant
import com.ledger.app.models.ParticipantRole
import com.ledger.app.models.ledger.Entry
import com.ledger.app.repositories.file.FileRepo
import com.ledger.app.repositories.file.FileMetadataRepo
import com.ledger.app.services.auth.AuthService
import com.ledger.app.services.file.FileService
import com.ledger.app.services.ledger.LedgerService
import com.ledger.app.utils.*
import com.ledger.app.utils.hash.HashProvider
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class FileServiceSpring(
    private val metadataRepo: FileMetadataRepo,
    private val fileRepo: FileRepo,
    private val ledgerService: LedgerService,
    private val authService: AuthService
) : FileService {
    private val FILES_SYSTEM = "files_service"
    private val FILES_LEDGER = "files_ledger"

    @Value("\${app.logLevel:INFO}")
    private lateinit var logLevelStr: String
    private lateinit var logger: ColorLogger

    @PostConstruct
    fun initialize() {
        logger = ColorLogger("FilesService", RGB.BLUE, logLevelStr)

        if (ledgerService.getLedger(FILES_LEDGER) == null) {
            ledgerService.createLedger(FILES_LEDGER, 2, HashProvider.getDefaultAlgorithm())
        }
    }

    private fun writeInUserLedger(userId: String, message: String, senders: List<String>, receivers: List<String>, relatedEntries: List<String>): Entry? {
        return try {
            val userLedgerName = "user_files_$userId"
            if (ledgerService.getLedger(userLedgerName) == null) {
                ledgerService.createLedger(userLedgerName, 10, HashProvider.getDefaultAlgorithm())
            }

            val allParticipants = mutableSetOf<String>()
            allParticipants.addAll(senders)
            allParticipants.addAll(receivers)

            val entry = ledgerService.createEntry(
                ledgerName = userLedgerName,
                content = message,
                senders = listOf(userId) + senders,
                recipients = receivers,
                relatedEntries = relatedEntries
            )

            entry
        } catch (e: Exception) {
            logger.error("Failed to write in user ledger: ${e.message}")
            null
        }
    }

    override fun saveFile(uploaderId: String, file: MultipartFile, senders: List<String>, receivers: List<String>): FileMetadata {
        try {
            val uploaderAndSenders = listOf(uploaderId) + senders

            val allParticipants = uploaderAndSenders + receivers
            for (participantId in allParticipants) {
                if (authService.getUserInfo(participantId) == null) {
                    throw IllegalArgumentException("User not found: $participantId")
                }
            }

            val collisions = metadataRepo.checkFileNameCollisionForParticipants(file.originalFilename!!, allParticipants)
            if (collisions.isNotEmpty()) {
                throw IllegalArgumentException("Filename collision detected for users: ${collisions.joinToString(", ")}")
            }

            val savedFile = fileRepo.saveFile(uploaderId, file)
            val entry = writeInUserLedger(uploaderId, "User $uploaderId uploaded file ${file.originalFilename} (${file.size} bytes)", senders, receivers, emptyList())
            val metadata = FileMetadata(
                id = UUID.randomUUID().toString(),
                originalFileName = file.originalFilename!!,
                actualFileName = savedFile.name,
                filePath = savedFile.path,
                fileSize = file.size,
                contentType = file.contentType,
                uploadedAt = entry!!.timestamp,
                uploaderId = uploaderId,
                senders = uploaderAndSenders,
                receivers = receivers,
                ledgerEntries = listOf(entry.id)
            )
            metadataRepo.saveFileMetadata(metadata)

            ledgerService.logSystemEvent(FILES_LEDGER, FILES_SYSTEM, uploaderId, "File uploaded: ${metadata.originalFileName} (${metadata.fileSize} bytes).")

            return metadata
        } catch (e: IllegalArgumentException) {
            logger.error("Validation error: ${e.message}")
            throw RuntimeException("Validation error: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Error while saving file: ${e.message}")
            throw RuntimeException("Could not save file: ${e.message}", e)
        }
    }

    override fun loadFile(userId: String, fileMetadataId: String): Resource {
        try {
            val metadata = metadataRepo.getFileMetadata(fileMetadataId)
                ?: throw IllegalArgumentException("File metadata not found: $fileMetadataId")

            if (!hasFileAccess(userId, fileMetadataId)) {
                throw SecurityException("User $userId does not have access to file ${metadata.originalFileName}")
            }

            val resource = fileRepo.loadFile(metadata.uploaderId, metadata.actualFileName)
            writeInUserLedger(userId, "User $userId downloaded file ${metadata.originalFileName}", listOf(userId), emptyList(), metadata.ledgerEntries)
            ledgerService.logSystemEvent(FILES_LEDGER, FILES_SYSTEM, userId, "File downloaded: ${metadata.originalFileName}")

            return resource
        } catch (e: SecurityException) {
            logger.error("Security violation while loading file: ${e.message}")
            throw RuntimeException("Access denied", e)
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid request: ${e.message}")
            throw RuntimeException("Invalid request: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Error while loading file: ${e.message}")
            throw RuntimeException("Could not load file: ${e.message}", e)
        }
    }

    override fun deleteFile(userId: String, fileMetadataId: String): Boolean {
         try {
             val metadata = metadataRepo.getFileMetadata(fileMetadataId)
             if (metadata == null) {
                logger.warn("File metadata not found for deletion: $fileMetadataId")
                return false
             }

            if (metadata.uploaderId != userId) {
                logger.warn("User $userId attempted to delete file they don't own: ${metadata.originalFileName}")
                throw SecurityException("Only the uploader can delete this file")
            }

            val success = fileRepo.deleteFile(metadata.uploaderId, metadata.actualFileName)
             val delMetadata = metadata.copy(wasDeleted = true)
             metadataRepo.updateFileMetadata(delMetadata)

            if (success) {
                logger.debug("File deleted successfully: ${metadata.originalFileName}")
                ledgerService.logSystemEvent(FILES_LEDGER, FILES_SYSTEM, userId, "File deleted: ${metadata.originalFileName}")
                writeInUserLedger(userId,"User $userId deleted file ${metadata.originalFileName}", metadata.senders, metadata.receivers, metadata.ledgerEntries)
            } else {
                logger.warn("Failed to delete file: ${metadata.originalFileName}")
            }
            return success
        } catch (e: SecurityException) {
            logger.error("Security violation: ${e.message}")
            throw RuntimeException("Access denied: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Error deleting file: ${e.message}")
            return false
        }
    }

    override fun getUserAccessibleFiles(userId: String): List<FileMetadata> {
        return try {
            val list = metadataRepo.getUserAccessibleFiles(userId)
            logger.debug("Found ${list.size} file metadatas")
            list
        } catch (e: Exception) {
            logger.error("Error getting accessible files for user $userId: ${e.message}")
            emptyList()
        }
    }

    override fun getFileMetadata(fileMetadataId: String): FileMetadata? {
        return try {
            metadataRepo.getFileMetadata(fileMetadataId)
        } catch (e: Exception) {
            logger.error("Error getting file metadata for ID $fileMetadataId: ${e.message}")
            null
        }
    }
    override fun hasFileAccess(userId: String, fileMetadataId: String): Boolean {
        return try {
            metadataRepo.hasFileAccess(userId, fileMetadataId)
        } catch (e: Exception) {
            logger.error("Error checking file access: ${e.message}")
            false
        }
    }
}