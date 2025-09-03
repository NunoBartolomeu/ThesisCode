package com.ledger.app.repositories.file

import com.ledger.app.models.FileMetadata
import com.ledger.app.models.FileParticipant

interface FileMetadataRepo {
    fun saveFileMetadata(metadata: FileMetadata): Boolean
    fun updateFileMetadata(metadata: FileMetadata): Boolean
    fun getFileMetadata(metadataId: String): FileMetadata?
    fun updateLedgerEntryId(metadataId: String, ledgerEntryId: String): Boolean
    fun getUserAccessibleFiles(userId: String): List<FileMetadata>
    fun hasFileAccess(userId: String, fileMetadataId: String): Boolean
    fun checkFileNameCollisionForParticipants(fileName: String, participants: List<String>): List<String>
    fun getFileParticipants(fileMetadataId: String): List<FileParticipant>
    fun removeFileMetadata(metadataId: String): Boolean
}
