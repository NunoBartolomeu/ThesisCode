package com.ledger.app.controllers

import com.ledger.app.dtos.FileDetailsDto
import com.ledger.app.services.files.FilesService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB
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
class FileController(private val filesService: FilesService) {

    private val logger = ColorLogger("FileController", RGB.BLUE_DARK, LogLevel.DEBUG)

    @PostMapping("/upload")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val user = authentication.principal
            val userId = getUserId(user)

            logger.info("Uploading file ${file.originalFilename} for user $userId")

            if (file.isEmpty) {
                return ResponseEntity.badRequest()
                    .body(mapOf("error" to "File is empty"))
            }

            val savedFile = filesService.saveFile(userId, file)

            logger.info("File uploaded successfully: ${savedFile.name}")
            ResponseEntity.ok(
                mapOf(
                    "message" to "File uploaded successfully",
                    "fileName" to savedFile.name,
                    "size" to savedFile.length()
                )
            )
        } catch (e: Exception) {
            logger.error("Error uploading file: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to upload file: ${e.message}"))
        }
    }

    @GetMapping("/download/{fileName}")
    fun downloadFile(
        @PathVariable fileName: String,
        authentication: Authentication
    ): ResponseEntity<Resource> {
        return try {
            val user = authentication.principal
            val userId = getUserId(user)

            logger.info("Downloading file $fileName for user $userId")

            val fileResource = filesService.loadFile(userId, fileName)

            if (!fileResource.exists()) {
                logger.warn("File not found: $fileName for user $userId")
                return ResponseEntity.notFound().build()
            }

            logger.info("File downloaded successfully: $fileName")
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
                .body(fileResource)

        } catch (e: Exception) {
            logger.error("Error downloading file: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/list")
    fun listUserFiles(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        return try {
            val user = authentication.principal
            val userId = getUserId(user)

            logger.debug("Listing files for user $userId")

            val files = filesService.listUserFiles(userId)

            ResponseEntity.ok(
                mapOf(
                    "files" to files.map { file ->
                        mapOf(
                            "name" to file.name,
                            "size" to file.length(),
                            "lastModified" to file.lastModified()
                        )
                    }
                )
            )
        } catch (e: Exception) {
            logger.error("Error listing files: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to list files: ${e.message}"))
        }
    }

    @GetMapping("/details/{fileName}")
    fun getFileDetails(
        @PathVariable fileName: String,
        authentication: Authentication
    ): ResponseEntity<FileDetailsDto> {
        return try {
            val user = authentication.principal
            val userId = getUserId(user)

            logger.debug("Getting file details for $fileName for user $userId")

            val fileDetails = filesService.getFileDetails(userId, fileName)

            if (fileDetails != null) {
                logger.info("File details retrieved successfully: $fileName")
                logger.debug("Files details: $fileDetails")
                ResponseEntity.ok(fileDetails)
            } else {
                logger.warn("File not found: $fileName for user $userId")
                ResponseEntity.notFound().build()
            }

        } catch (e: Exception) {
            logger.error("Error getting file details: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @DeleteMapping("/delete/{fileName}")
    fun deleteFile(
        @PathVariable fileName: String,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        return try {
            val user = authentication.principal
            val userId = getUserId(user)

            logger.info("Deleting file $fileName for user $userId")

            val deleted = filesService.deleteFile(userId, fileName)

            if (deleted) {
                logger.info("File deleted successfully: $fileName")
                ResponseEntity.ok(mapOf("message" to "File deleted successfully"))
            } else {
                logger.warn("File not found for deletion: $fileName")
                ResponseEntity.notFound().build()
            }

        } catch (e: Exception) {
            logger.error("Error deleting file: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to delete file: ${e.message}"))
        }
    }

    private fun getUserId(user: Any): String {
        // Assuming your user object has an id or userId property
        // Adjust this based on your actual user entity structure
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
                // Fallback to email or toString if no id field found
                user.toString()
            }
        }
    }
}