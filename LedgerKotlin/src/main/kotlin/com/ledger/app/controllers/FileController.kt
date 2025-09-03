package com.ledger.app.controllers

import com.ledger.app.dtos.FileInfoDTO
import com.ledger.app.dtos.FileListResponse
import com.ledger.app.dtos.FileMetadataDto
import com.ledger.app.services.file.FileService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.RGB
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/file")
class FileController(private val fileService: FileService) {
    @Value("\${app.logLevel:INFO}")
    private lateinit var logLevelStr: String
    private lateinit var logger: ColorLogger

    @PostConstruct
    fun initialize() {
        logger = ColorLogger("FileController", RGB.BLUE_DARK, logLevelStr)
    }

    @PostMapping("/upload")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("senders", required = false) senders: List<String>?,
        @RequestParam("receivers", required = false) receivers: List<String>?,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        return try {
            val user = authentication.principal
            val userId = getUserId(user)

            logger.info("Uploading file ${file.originalFilename} for user $userId")

            if (file.isEmpty) {
                return ResponseEntity.badRequest()
                    .body(mapOf("error" to "File is empty"))
            }

            val fileMetadata = fileService.saveFile(
                uploaderId = userId,
                file = file,
                senders = senders ?: emptyList(),
                receivers = receivers ?: emptyList()
            )

            logger.info("File uploaded successfully with ID: ${fileMetadata.id}")
            ResponseEntity.ok(mapOf(
                "message" to "File uploaded successfully",
                "fileId" to fileMetadata.id,
                "fileName" to fileMetadata.originalFileName
            ))
        } catch (e: Exception) {
            logger.error("Error uploading file: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to upload file: ${e.message}"))
        }
    }

    @GetMapping("/download/{fileId}")
    fun downloadFile(
        @PathVariable fileId: String,
        authentication: Authentication
    ): ResponseEntity<Resource> {
        return try {
            val user = authentication.principal
            val userId = getUserId(user)

            logger.info("Downloading file with ID $fileId for user $userId")

            // Check if user has access to the file
            if (!fileService.hasFileAccess(userId, fileId)) {
                logger.warn("User $userId does not have access to file $fileId")
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }

            val fileResource = fileService.loadFile(userId, fileId)
            val fileMetadata = fileService.getFileMetadata(fileId)

            if (!fileResource.exists() || fileMetadata == null) {
                logger.warn("File not found: $fileId for user $userId")
                return ResponseEntity.notFound().build()
            }

            logger.info("File downloaded successfully: ${fileMetadata.originalFileName}")
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileMetadata.originalFileName}\"")
                .body(fileResource)

        } catch (e: Exception) {
            logger.error("Error downloading file: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/list")
    fun listUserFiles(authentication: Authentication): ResponseEntity<FileListResponse> {
        return try {
            val user = authentication.principal
            val userId = getUserId(user)

            logger.debug("Listing accessible files for user $userId")

            val fileMetadataList = fileService.getUserAccessibleFiles(userId)

            val fileDTOs = fileMetadataList.map {
                FileInfoDTO(
                    id = it.id,
                    name = it.originalFileName,
                    size = it.fileSize,
                    wasDeleted = it.wasDeleted
                )
            }

            logger.debug("Found ${fileDTOs.size} files with ${fileDTOs.count{it.wasDeleted}} deleted")


            ResponseEntity.ok(FileListResponse(fileDTOs))
        } catch (e: Exception) {
            logger.error("Error listing files: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/metadata/{fileId}")
    fun getFileMetadata(
        @PathVariable fileId: String,
        authentication: Authentication
    ): ResponseEntity<FileMetadataDto> {
        return try {
            val user = authentication.principal
            val userId = getUserId(user)

            logger.debug("Getting file metadata for ID $fileId for user $userId")

            // Check if user has access to the file
            if (!fileService.hasFileAccess(userId, fileId)) {
                logger.warn("User $userId does not have access to file $fileId")
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }

            val fileMetadata = fileService.getFileMetadata(fileId)

            if (fileMetadata != null) {
                // Convert FileMetadata to FileMetadataDto
                val fileMetadataDto = FileMetadataDto(
                    id = fileMetadata.id,
                    originalFileName = fileMetadata.originalFileName,
                    actualFileName = fileMetadata.actualFileName,
                    filePath = fileMetadata.filePath,
                    fileSize = fileMetadata.fileSize,
                    contentType = fileMetadata.contentType,
                    uploadedAt = fileMetadata.uploadedAt,
                    uploaderId = fileMetadata.uploaderId,
                    senders = fileMetadata.senders,
                    receivers = fileMetadata.receivers,
                    ledgerEntries = fileMetadata.ledgerEntries,
                    wasDeleted = fileMetadata.wasDeleted
                )

                logger.info("File metadata retrieved successfully: ${fileMetadata.originalFileName}")
                logger.debug("File metadata: $fileMetadataDto")
                ResponseEntity.ok(fileMetadataDto)
            } else {
                logger.warn("File not found: $fileId")
                ResponseEntity.notFound().build()
            }

        } catch (e: Exception) {
            logger.error("Error getting file metadata: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @DeleteMapping("/delete/{fileId}")
    fun deleteFile(
        @PathVariable fileId: String,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        return try {
            val user = authentication.principal
            val userId = getUserId(user)

            logger.info("Deleting file with ID $fileId for user $userId")

            val deleted = fileService.deleteFile(userId, fileId)

            if (deleted) {
                logger.info("File deleted successfully: $fileId")
                ResponseEntity.ok(mapOf("message" to "File deleted successfully"))
            } else {
                logger.warn("File not found for deletion or user lacks permission: $fileId")
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "File not found or access denied"))
            }

        } catch (e: Exception) {
            logger.error("Error deleting file: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to delete file: ${e.message}"))
        }
    }

    private fun getUserId(user: Any): String {
        return when {
            user.javaClass.getDeclaredField("id") != null -> {
                val field = user.javaClass.getDeclaredField("id")
                field.isAccessible = true
                field.get(user).toString()
            }
            user.javaClass.getDeclaredField("userId") != null -> {
                val field = user.javaClass.getDeclaredField("userId")
                field.isAccessible = true
                field.get(user).toString()
            }
            else -> {
                user.toString()
            }
        }
    }
}