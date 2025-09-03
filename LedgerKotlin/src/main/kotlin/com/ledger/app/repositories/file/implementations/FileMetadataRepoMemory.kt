package com.ledger.app.repositories.file.implementations

import com.ledger.app.models.FileMetadata
import com.ledger.app.models.FileParticipant
import com.ledger.app.models.ParticipantRole
import com.ledger.app.repositories.file.FileMetadataRepo
import org.springframework.stereotype.Repository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Repository
class FileMetadataRepoMemory : FileMetadataRepo {
    private val metadataStorage = ConcurrentHashMap<String, FileMetadata>()
    private val participantsStorage = ConcurrentHashMap<String, MutableList<FileParticipant>>()

    override fun saveFileMetadata(metadata: FileMetadata): Boolean {
        return try {
            metadataStorage[metadata.id] = metadata

            val fileParticipants = mutableListOf<FileParticipant>()

            fileParticipants.add(FileParticipant(metadata.uploaderId, metadata.id, ParticipantRole.UPLOADER))
            metadata.senders.forEach { fileParticipants.add(FileParticipant(it, metadata.id, ParticipantRole.SENDER)) }
            metadata.receivers.forEach { fileParticipants.add(FileParticipant(it, metadata.id, ParticipantRole.RECEIVER)) }

            println("File participants: $participantsStorage")
            addFileParticipants(fileParticipants)
            println("Updated file participants: $participantsStorage")

            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getFileMetadata(metadataId: String): FileMetadata? {
        return metadataStorage[metadataId]
    }

    override fun updateFileMetadata(metadata: FileMetadata): Boolean {
        return if (metadataStorage.containsKey(metadata.id)) {
            metadataStorage[metadata.id] = metadata
            true
        } else {
            false
        }
    }

    override fun updateLedgerEntryId(metadataId: String, ledgerEntryId: String): Boolean {
        val existingMetadata = metadataStorage[metadataId] ?: return false
        val updatedMetadata = existingMetadata.copy(ledgerEntries = existingMetadata.ledgerEntries + ledgerEntryId)
        metadataStorage[metadataId] = updatedMetadata
        return true
    }

    private fun addFileParticipants(participants: List<FileParticipant>): Boolean {
        return try {
            participants.forEach { participant ->
                val existingParticipants = participantsStorage.getOrPut(participant.fileMetadataId) { mutableListOf() }
                val existingParticipant = existingParticipants.find {
                    it.userId == participant.userId && it.fileMetadataId == participant.fileMetadataId
                }
                if (existingParticipant == null) {
                    existingParticipants.add(participant)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getUserAccessibleFiles(userId: String): List<FileMetadata> {
        println("File participants for listing: $participantsStorage")
        println("User id: $userId")
        val accessibleFileIds = participantsStorage.values
            .flatten()
            .filter { it.userId == userId }
            .map { it.fileMetadataId }
            .toSet()
        println("User has ${accessibleFileIds.size} accessible files")
        return accessibleFileIds.mapNotNull { metadataStorage[it] }
    }

    override fun hasFileAccess(userId: String, fileMetadataId: String): Boolean {
        val participants = participantsStorage[fileMetadataId] ?: return false
        return participants.any { it.userId == userId }
    }

    override fun checkFileNameCollisionForParticipants(fileName: String, participants: List<String>): List<String> {
        val collisions = mutableListOf<String>()

        participants.forEach { participantId ->
            // Get all files accessible to this participant
            val userFiles = getUserAccessibleFiles(participantId)

            // Check if any of their accessible files has the same original filename
            val hasCollision = userFiles.any { it.originalFileName == fileName }
            if (hasCollision) {
                collisions.add(participantId)
            }
        }

        return collisions
    }

    override fun getFileParticipants(fileMetadataId: String): List<FileParticipant> {
        return participantsStorage[fileMetadataId]?.toList() ?: emptyList()
    }

    override fun removeFileMetadata(metadataId: String): Boolean {
        return try {
            metadataStorage.remove(metadataId)
            participantsStorage.remove(metadataId)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun generateId(): String {
        return UUID.randomUUID().toString()
    }
}