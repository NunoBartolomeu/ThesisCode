package com.ledger.app.repositories.file.implementations

import com.ledger.app.models.FileMetadata
import com.ledger.app.models.FileParticipant
import com.ledger.app.models.ParticipantRole
import com.ledger.app.repositories.file.FileMetadataRepo
import org.springframework.stereotype.Repository
import java.sql.Connection
import javax.sql.DataSource
import java.util.*

@Repository
class FileMetadataRepoJDBC(
    private val dataSource: DataSource
) : FileMetadataRepo {

    private fun getConnection(): Connection = dataSource.connection

    init {
        // create tables if they don't exist
        getConnection().use { conn ->
            conn.createStatement().use { st ->
                st.execute("""
                    CREATE TABLE IF NOT EXISTS file_metadata (
                        id VARCHAR PRIMARY KEY,
                        original_file_name VARCHAR NOT NULL,
                        actual_file_name VARCHAR NOT NULL,
                        file_path VARCHAR NOT NULL,
                        file_size BIGINT NOT NULL,
                        content_type VARCHAR,
                        uploaded_at BIGINT NOT NULL,
                        uploader_id VARCHAR NOT NULL,
                        was_deleted BOOLEAN NOT NULL DEFAULT FALSE
                    )
                """.trimIndent())

                st.execute("""
                    CREATE TABLE IF NOT EXISTS metadata_entries (
                        metadata_id VARCHAR NOT NULL REFERENCES file_metadata(id) ON DELETE CASCADE,
                        ledger_entry_id VARCHAR NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
                        PRIMARY KEY (metadata_id, ledger_entry_id)
                    )
                """.trimIndent())
            }
        }
    }

    override fun saveFileMetadata(metadata: FileMetadata): Boolean {
        val id = if (metadata.id.isBlank()) generateId() else metadata.id
        return try {
            getConnection().use { conn ->
                conn.autoCommit = false
                conn.prepareStatement(
                    """
                    INSERT INTO file_metadata 
                    (id, original_file_name, actual_file_name, file_path, file_size, content_type, uploaded_at, uploader_id, was_deleted)
                    VALUES (?,?,?,?,?,?,?,?,?)
                    """
                ).use { ps ->
                    ps.setString(1, id)
                    ps.setString(2, metadata.originalFileName)
                    ps.setString(3, metadata.actualFileName)
                    ps.setString(4, metadata.filePath)
                    ps.setLong(5, metadata.fileSize)
                    ps.setString(6, metadata.contentType)
                    ps.setLong(7, metadata.uploadedAt)
                    ps.setString(8, metadata.uploaderId)
                    ps.setBoolean(9, metadata.wasDeleted)
                    ps.executeUpdate()
                }

                // link ledger entries (may be empty)
                conn.prepareStatement(
                    "INSERT INTO metadata_entries (metadata_id, ledger_entry_id) VALUES (?,?) ON CONFLICT DO NOTHING"
                ).use { ps ->
                    metadata.ledgerEntries.forEach { entryId ->
                        ps.setString(1, id)
                        ps.setString(2, entryId)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }

                conn.commit()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getFileMetadata(metadataId: String): FileMetadata? {
        return try {
            getConnection().use { conn ->
                conn.prepareStatement(
                    """
                    SELECT id, original_file_name, actual_file_name, file_path, file_size, content_type, uploaded_at, uploader_id, was_deleted
                    FROM file_metadata
                    WHERE id = ?
                    """
                ).use { ps ->
                    ps.setString(1, metadataId)
                    val rs = ps.executeQuery()
                    if (!rs.next()) return null

                    val ledgerEntries = fetchLedgerEntries(conn, metadataId)
                    val senders = fetchSenders(conn, metadataId)
                    val recipients = fetchRecipients(conn, metadataId)

                    FileMetadata(
                        id = rs.getString("id"),
                        originalFileName = rs.getString("original_file_name"),
                        actualFileName = rs.getString("actual_file_name"),
                        filePath = rs.getString("file_path"),
                        fileSize = rs.getLong("file_size"),
                        contentType = rs.getString("content_type"),
                        uploadedAt = rs.getLong("uploaded_at"),
                        uploaderId = rs.getString("uploader_id"),
                        senders = senders,
                        receivers = recipients,
                        ledgerEntries = ledgerEntries,
                        wasDeleted = rs.getBoolean("was_deleted")
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun updateFileMetadata(metadata: FileMetadata): Boolean {
        return try {
            getConnection().use { conn ->
                conn.autoCommit = false

                // ensure exists
                conn.prepareStatement("SELECT 1 FROM file_metadata WHERE id = ?").use { ps ->
                    ps.setString(1, metadata.id)
                    val rs = ps.executeQuery()
                    if (!rs.next()) {
                        conn.rollback()
                        return false
                    }
                }

                conn.prepareStatement(
                    """
                    UPDATE file_metadata SET
                      original_file_name = ?,
                      actual_file_name = ?,
                      file_path = ?,
                      file_size = ?,
                      content_type = ?,
                      uploaded_at = ?,
                      uploader_id = ?,
                      was_deleted = ?
                    WHERE id = ?
                    """
                ).use { ps ->
                    ps.setString(1, metadata.originalFileName)
                    ps.setString(2, metadata.actualFileName)
                    ps.setString(3, metadata.filePath)
                    ps.setLong(4, metadata.fileSize)
                    ps.setString(5, metadata.contentType)
                    ps.setLong(6, metadata.uploadedAt)
                    ps.setString(7, metadata.uploaderId)
                    ps.setBoolean(8, metadata.wasDeleted)
                    ps.setString(9, metadata.id)
                    ps.executeUpdate()
                }

                // Replace metadata_entries with the provided list (keeps authoritative mapping from caller)
                conn.prepareStatement("DELETE FROM metadata_entries WHERE metadata_id = ?").use { ps ->
                    ps.setString(1, metadata.id)
                    ps.executeUpdate()
                }

                conn.prepareStatement("INSERT INTO metadata_entries (metadata_id, ledger_entry_id) VALUES (?,?) ON CONFLICT DO NOTHING").use { ps ->
                    metadata.ledgerEntries.forEach { entryId ->
                        ps.setString(1, metadata.id)
                        ps.setString(2, entryId)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }

                conn.commit()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun updateLedgerEntryId(metadataId: String, ledgerEntryId: String): Boolean {
        return try {
            getConnection().use { conn ->
                // ensure metadata exists
                conn.prepareStatement("SELECT 1 FROM file_metadata WHERE id = ?").use { ps ->
                    ps.setString(1, metadataId)
                    val rs = ps.executeQuery()
                    if (!rs.next()) return false
                }

                conn.prepareStatement(
                    "INSERT INTO metadata_entries (metadata_id, ledger_entry_id) VALUES (?,?) ON CONFLICT DO NOTHING"
                ).use { ps ->
                    ps.setString(1, metadataId)
                    ps.setString(2, ledgerEntryId)
                    ps.executeUpdate()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getUserAccessibleFiles(userId: String): List<FileMetadata> {
        val ids = mutableSetOf<String>()
        try {
            getConnection().use { conn ->
                // uploader-owned files
                conn.prepareStatement("SELECT id FROM file_metadata WHERE uploader_id = ?").use { ps ->
                    ps.setString(1, userId)
                    val rs = ps.executeQuery()
                    while (rs.next()) ids.add(rs.getString("id"))
                }

                // files referenced by entries where user is sender
                conn.prepareStatement(
                    """
                    SELECT DISTINCT me.metadata_id
                    FROM metadata_entries me
                    JOIN entry_senders es ON es.entry_id = me.ledger_entry_id
                    WHERE es.user_id = ?
                    """
                ).use { ps ->
                    ps.setString(1, userId)
                    val rs = ps.executeQuery()
                    while (rs.next()) ids.add(rs.getString("metadata_id"))
                }

                // files referenced by entries where user is recipient
                conn.prepareStatement(
                    """
                    SELECT DISTINCT me.metadata_id
                    FROM metadata_entries me
                    JOIN entry_recipients er ON er.entry_id = me.ledger_entry_id
                    WHERE er.user_id = ?
                    """
                ).use { ps ->
                    ps.setString(1, userId)
                    val rs = ps.executeQuery()
                    while (rs.next()) ids.add(rs.getString("metadata_id"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        return ids.mapNotNull { getFileMetadata(it) }
    }

    override fun hasFileAccess(userId: String, fileMetadataId: String): Boolean {
        return try {
            getConnection().use { conn ->
                // uploader?
                conn.prepareStatement("SELECT uploader_id FROM file_metadata WHERE id = ?").use { ps ->
                    ps.setString(1, fileMetadataId)
                    val rs = ps.executeQuery()
                    if (!rs.next()) return false
                    val uploader = rs.getString("uploader_id")
                    if (uploader == userId) return true
                }

                // sender?
                conn.prepareStatement(
                    """
                    SELECT 1 FROM entry_senders 
                    WHERE user_id = ? AND entry_id IN (
                      SELECT ledger_entry_id FROM metadata_entries WHERE metadata_id = ?
                    ) LIMIT 1
                    """
                ).use { ps ->
                    ps.setString(1, userId)
                    ps.setString(2, fileMetadataId)
                    val rs = ps.executeQuery()
                    if (rs.next()) return true
                }

                // recipient?
                conn.prepareStatement(
                    """
                    SELECT 1 FROM entry_recipients 
                    WHERE user_id = ? AND entry_id IN (
                      SELECT ledger_entry_id FROM metadata_entries WHERE metadata_id = ?
                    ) LIMIT 1
                    """
                ).use { ps ->
                    ps.setString(1, userId)
                    ps.setString(2, fileMetadataId)
                    val rs = ps.executeQuery()
                    if (rs.next()) return true
                }

                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun checkFileNameCollisionForParticipants(fileName: String, participants: List<String>): List<String> {
        val collisions = mutableListOf<String>()
        try {
            getConnection().use { conn ->
                participants.forEach { participantId ->
                    // check uploader-owned files
                    conn.prepareStatement(
                        """
                        SELECT 1 FROM file_metadata 
                        WHERE original_file_name = ? AND uploader_id = ? LIMIT 1
                        """
                    ).use { ps ->
                        ps.setString(1, fileName)
                        ps.setString(2, participantId)
                        val rs = ps.executeQuery()
                        if (rs.next()) {
                            collisions.add(participantId)
                            return@forEach
                        }
                    }

                    // check files where participant is sender or recipient
                    conn.prepareStatement(
                        """
                        SELECT 1 FROM file_metadata fm
                        WHERE fm.original_file_name = ?
                          AND fm.id IN (
                            SELECT me.metadata_id FROM metadata_entries me
                            WHERE me.ledger_entry_id IN (
                              SELECT entry_id FROM entry_senders WHERE user_id = ?
                              UNION
                              SELECT entry_id FROM entry_recipients WHERE user_id = ?
                            )
                          )
                        LIMIT 1
                        """
                    ).use { ps ->
                        ps.setString(1, fileName)
                        ps.setString(2, participantId)
                        ps.setString(3, participantId)
                        val rs = ps.executeQuery()
                        if (rs.next()) collisions.add(participantId)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return collisions
    }

    override fun getFileParticipants(fileMetadataId: String): List<FileParticipant> {
        val participants = mutableListOf<FileParticipant>()
        val seen = mutableSetOf<String>()
        try {
            getConnection().use { conn ->
                // uploader first
                conn.prepareStatement("SELECT uploader_id FROM file_metadata WHERE id = ?").use { ps ->
                    ps.setString(1, fileMetadataId)
                    val rs = ps.executeQuery()
                    if (rs.next()) {
                        val uploader = rs.getString("uploader_id")
                        if (!uploader.isNullOrBlank()) {
                            participants.add(FileParticipant(uploader, fileMetadataId, ParticipantRole.UPLOADER))
                            seen.add(uploader)
                        }
                    }
                }

                // senders (distinct)
                conn.prepareStatement(
                    """
                    SELECT DISTINCT es.user_id
                    FROM entry_senders es
                    WHERE es.entry_id IN (SELECT ledger_entry_id FROM metadata_entries WHERE metadata_id = ?)
                    """
                ).use { ps ->
                    ps.setString(1, fileMetadataId)
                    val rs = ps.executeQuery()
                    while (rs.next()) {
                        val uid = rs.getString("user_id")
                        if (!seen.contains(uid)) {
                            participants.add(FileParticipant(uid, fileMetadataId, ParticipantRole.SENDER))
                            seen.add(uid)
                        }
                    }
                }

                // recipients (distinct)
                conn.prepareStatement(
                    """
                    SELECT DISTINCT er.user_id
                    FROM entry_recipients er
                    WHERE er.entry_id IN (SELECT ledger_entry_id FROM metadata_entries WHERE metadata_id = ?)
                    """
                ).use { ps ->
                    ps.setString(1, fileMetadataId)
                    val rs = ps.executeQuery()
                    while (rs.next()) {
                        val uid = rs.getString("user_id")
                        if (!seen.contains(uid)) {
                            participants.add(FileParticipant(uid, fileMetadataId, ParticipantRole.RECEIVER))
                            seen.add(uid)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return participants
    }

    override fun removeFileMetadata(metadataId: String): Boolean {
        return try {
            getConnection().use { conn ->
                conn.prepareStatement("DELETE FROM file_metadata WHERE id = ?").use { ps ->
                    ps.setString(1, metadataId)
                    val deleted = ps.executeUpdate()
                    deleted > 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ----------------------
    // Helpers
    // ----------------------
    private fun fetchLedgerEntries(conn: Connection, metadataId: String): List<String> {
        val list = mutableListOf<String>()
        conn.prepareStatement("SELECT ledger_entry_id FROM metadata_entries WHERE metadata_id = ? ORDER BY ledger_entry_id").use { ps ->
            ps.setString(1, metadataId)
            val rs = ps.executeQuery()
            while (rs.next()) {
                list.add(rs.getString("ledger_entry_id"))
            }
        }
        return list
    }

    private fun fetchSenders(conn: Connection, metadataId: String): List<String> {
        val list = mutableListOf<String>()
        conn.prepareStatement(
            """
            SELECT DISTINCT es.user_id
            FROM entry_senders es
            WHERE es.entry_id IN (SELECT ledger_entry_id FROM metadata_entries WHERE metadata_id = ?)
            """
        ).use { ps ->
            ps.setString(1, metadataId)
            val rs = ps.executeQuery()
            while (rs.next()) list.add(rs.getString("user_id"))
        }
        return list
    }

    private fun fetchRecipients(conn: Connection, metadataId: String): List<String> {
        val list = mutableListOf<String>()
        conn.prepareStatement(
            """
            SELECT DISTINCT er.user_id
            FROM entry_recipients er
            WHERE er.entry_id IN (SELECT ledger_entry_id FROM metadata_entries WHERE metadata_id = ?)
            """
        ).use { ps ->
            ps.setString(1, metadataId)
            val rs = ps.executeQuery()
            while (rs.next()) list.add(rs.getString("user_id"))
        }
        return list
    }

    private fun generateId(): String = UUID.randomUUID().toString()
}
