package com.ledger.app.repositories.ledger.implementations

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ledger.app.models.ledger.Entry
import com.ledger.app.models.ledger.Ledger
import com.ledger.app.models.ledger.LedgerConfig
import com.ledger.app.models.ledger.Page
import com.ledger.app.repositories.ledger.LedgerRepo
import org.postgresql.util.PGobject
import org.springframework.stereotype.Repository
import java.sql.Connection
import java.util.concurrent.ConcurrentLinkedQueue
import javax.sql.DataSource

@Repository
class LedgerRepoJDBC(
    private val dataSource: DataSource
) : LedgerRepo {

    private val mapper = jacksonObjectMapper()

    private fun getConnection(): Connection = dataSource.connection

    private fun toJsonb(value: Any): PGobject {
        val obj = PGobject()
        obj.type = "jsonb"
        obj.value = mapper.writeValueAsString(value)
        return obj
    }

    init {
        getConnection().use { conn ->
            conn.createStatement().use { st ->
                // Ledger table
                st.execute("""
                    CREATE TABLE IF NOT EXISTS ledgers (
                        name VARCHAR PRIMARY KEY,
                        entries_per_page INT NOT NULL,
                        hash_algorithm VARCHAR NOT NULL
                    )
                """.trimIndent())

                // Page table
                st.execute("""
                    CREATE TABLE IF NOT EXISTS pages (
                        ledger_name VARCHAR NOT NULL REFERENCES ledgers(name) ON DELETE CASCADE,
                        number INT NOT NULL,
                        timestamp BIGINT NOT NULL,
                        previous_hash VARCHAR,
                        merkle_root VARCHAR NOT NULL,
                        hash VARCHAR NOT NULL,
                        PRIMARY KEY (ledger_name, number)
                    )
                """.trimIndent())

                // Entry table
                st.execute("""
                    CREATE TABLE IF NOT EXISTS entries (
                        id VARCHAR PRIMARY KEY,
                        ledger_name VARCHAR NOT NULL REFERENCES ledgers(name) ON DELETE CASCADE,
                        timestamp BIGINT NOT NULL,
                        content TEXT NOT NULL,
                        hash VARCHAR NOT NULL,
                        keywords JSONB NOT NULL
                    )
                """.trimIndent())

                // Entry Senders
                st.execute("""
                    CREATE TABLE IF NOT EXISTS entry_senders (
                        entry_id VARCHAR NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
                        user_id VARCHAR NOT NULL,
                        public_key TEXT,
                        sig_data TEXT,
                        algorithm VARCHAR,
                        PRIMARY KEY (entry_id, user_id)
                    )
                """.trimIndent())

                // Entry Recipients
                st.execute("""
                    CREATE TABLE IF NOT EXISTS entry_recipients (
                        entry_id VARCHAR NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
                        user_id VARCHAR NOT NULL,
                        PRIMARY KEY (entry_id, user_id)
                    )
                """.trimIndent())

                // Related entries
                st.execute("""
                    CREATE TABLE IF NOT EXISTS related_entries (
                        entry_id VARCHAR NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
                        related_entry_id VARCHAR NOT NULL,
                        PRIMARY KEY (entry_id, related_entry_id)
                    )
                """.trimIndent())

                // Entries in pages
                st.execute("""
                    CREATE TABLE IF NOT EXISTS page_entries (
                        ledger_name VARCHAR NOT NULL REFERENCES ledgers(name) ON DELETE CASCADE,
                        page_num INT NOT NULL,
                        entry_id VARCHAR NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
                        PRIMARY KEY (ledger_name, page_num, entry_id)
                    )
                """.trimIndent())
            }
        }
    }

    // --- Ledger methods ---
    override fun getAllLedgers(): List<LedgerConfig> = getConnection().use { conn ->
        conn.prepareStatement("SELECT name, entries_per_page, hash_algorithm FROM ledgers").use { ps ->
            val rs = ps.executeQuery()
            val list = mutableListOf<LedgerConfig>()
            while (rs.next()) {
                list.add(
                    LedgerConfig(
                        name = rs.getString("name"),
                        entriesPerPage = rs.getInt("entries_per_page"),
                        hashAlgorithm = rs.getString("hash_algorithm")
                    )
                )
            }
            list
        }
    }

    override fun createLedger(ledger: Ledger) {
        getConnection().use { conn ->
            conn.prepareStatement(
                "INSERT INTO ledgers (name, entries_per_page, hash_algorithm) VALUES (?,?,?)"
            ).use { ps ->
                ps.setString(1, ledger.config.name)
                ps.setInt(2, ledger.config.entriesPerPage)
                ps.setString(3, ledger.config.hashAlgorithm)
                ps.executeUpdate()
            }
        }
    }

    override fun readLedger(ledgerName: String): Ledger? = getConnection().use { conn ->
        conn.prepareStatement("SELECT name, entries_per_page, hash_algorithm FROM ledgers WHERE name=?").use { ps ->
            ps.setString(1, ledgerName)
            val rs = ps.executeQuery()
            if (!rs.next()) return null
            val config = LedgerConfig(
                name = rs.getString("name"),
                entriesPerPage = rs.getInt("entries_per_page"),
                hashAlgorithm = rs.getString("hash_algorithm")
            )

            // Load pages
            val pages = ConcurrentLinkedQueue<Page>()
            conn.prepareStatement("SELECT * FROM pages WHERE ledger_name=? ORDER BY number").use { ps2 ->
                ps2.setString(1, ledgerName)
                val rs2 = ps2.executeQuery()
                while (rs2.next()) {
                    val entries = readEntriesForPage(conn, ledgerName, rs2.getInt("number"))
                    pages.add(
                        Page(
                            ledgerName = rs2.getString("ledger_name"),
                            number = rs2.getInt("number"),
                            timestamp = rs2.getLong("timestamp"),
                            previousHash = rs2.getString("previous_hash"),
                            merkleRoot = rs2.getString("merkle_root"),
                            hash = rs2.getString("hash"),
                            entries = entries.toMutableList()
                        )
                    )
                }
            }

            return Ledger(config = config, pages = pages)
        }
    }

    // --- Page methods ---
    override fun createPage(page: Page) = getConnection().use { conn ->
        conn.prepareStatement(
            "INSERT INTO pages (ledger_name, number, timestamp, previous_hash, merkle_root, hash) VALUES (?,?,?,?,?,?)"
        ).use { ps ->
            ps.setString(1, page.ledgerName)
            ps.setInt(2, page.number)
            ps.setLong(3, page.timestamp)
            ps.setString(4, page.previousHash)
            ps.setString(5, page.merkleRoot)
            ps.setString(6, page.hash)
            ps.executeUpdate()
        }

        // Map entries to page
        page.entries.forEach { entry ->
            conn.prepareStatement(
                "INSERT INTO page_entries (ledger_name, page_num, entry_id) VALUES (?,?,?)"
            ).use { ps ->
                ps.setString(1, page.ledgerName)
                ps.setInt(2, page.number)
                ps.setString(3, entry.id)
                ps.executeUpdate()
            }
        }
    }

    override fun readPage(ledgerName: String, pageNumber: Int): Page? = getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM pages WHERE ledger_name=? AND number=?").use { ps ->
            ps.setString(1, ledgerName)
            ps.setInt(2, pageNumber)
            val rs = ps.executeQuery()
            if (!rs.next()) return null
            val entries = readEntriesForPage(conn, ledgerName, pageNumber)
            Page(
                ledgerName = rs.getString("ledger_name"),
                number = rs.getInt("number"),
                timestamp = rs.getLong("timestamp"),
                previousHash = rs.getString("previous_hash"),
                merkleRoot = rs.getString("merkle_root"),
                hash = rs.getString("hash"),
                entries = entries.toMutableList()
            )
        }
    }

    override fun updatePageForTamperEvidenceTesting(page: Page) {
        getConnection().use { conn ->
            conn.prepareStatement("UPDATE pages SET merkle_root=?, hash=? WHERE ledger_name=? AND number=?").use { ps ->
                ps.setString(1, page.merkleRoot)
                ps.setString(2, page.hash)
                ps.setString(3, page.ledgerName)
                ps.setInt(4, page.number)
                ps.executeUpdate()
            }
        }
    }

    // --- Entry methods ---
    override fun createEntry(entry: Entry) {
        getConnection().use { conn ->
            conn.prepareStatement(
                "INSERT INTO entries (id, ledger_name, timestamp, content, hash, keywords) VALUES (?,?,?,?,?,?)"
            ).use { ps ->
                ps.setString(1, entry.id)
                ps.setString(2, entry.ledgerName)
                ps.setLong(3, entry.timestamp)
                ps.setString(4, entry.content)
                ps.setString(5, entry.hash)
                ps.setObject(6, toJsonb(entry.keywords))
                ps.executeUpdate()
            }

            // Senders
            val sigMap = entry.signatures.associateBy { it.signerId }
            entry.senders.forEach { userId ->
                val sig = sigMap[userId]
                conn.prepareStatement(
                    "INSERT INTO entry_senders (entry_id, user_id, public_key, sig_data, algorithm) VALUES (?,?,?,?,?)"
                ).use { ps ->
                    ps.setString(1, entry.id)
                    ps.setString(2, userId)
                    ps.setString(3, sig?.publicKey)
                    ps.setString(4, sig?.signatureData)
                    ps.setString(5, sig?.algorithm)
                    ps.executeUpdate()
                }
            }

            // Recipients
            entry.recipients.forEach { recipient ->
                conn.prepareStatement(
                    "INSERT INTO entry_recipients (entry_id, user_id) VALUES (?,?)"
                ).use { ps ->
                    ps.setString(1, entry.id)
                    ps.setString(2, recipient)
                    ps.executeUpdate()
                }
            }

            // Related entries
            entry.relatedEntries.forEach { related ->
                conn.prepareStatement(
                    "INSERT INTO related_entries (entry_id, related_entry_id) VALUES (?,?)"
                ).use { ps ->
                    ps.setString(1, entry.id)
                    ps.setString(2, related)
                    ps.executeUpdate()
                }
            }
        }
    }

    override fun readEntry(entryId: String): Entry? = getConnection().use { conn ->
        conn.prepareStatement(
            "SELECT id, ledger_name, timestamp, content, hash, keywords FROM entries WHERE id=?"
        ).use { ps ->
            ps.setString(1, entryId)
            val rs = ps.executeQuery()
            if (!rs.next()) return null

            // keywords: tolerate NULL / empty / malformed
            val rawKeywords = rs.getString("keywords")
            val keywords: List<String> = try {
                if (rawKeywords.isNullOrBlank()) emptyList()
                else mapper.readValue(rawKeywords)
            } catch (_: Exception) {
                emptyList()
            }

            // Senders + Signatures
            val senders = mutableListOf<String>()
            val signatures = mutableListOf<Entry.Signature>()
            conn.prepareStatement(
                "SELECT user_id, public_key, sig_data, algorithm FROM entry_senders WHERE entry_id=?"
            ).use { ps2 ->
                ps2.setString(1, entryId)
                val rs2 = ps2.executeQuery()
                while (rs2.next()) {
                    val userId = rs2.getString("user_id")!!  // NOT NULL by schema
                    senders.add(userId)

                    val pk = rs2.getString("public_key")
                    val sigData = rs2.getString("sig_data")
                    val algo = rs2.getString("algorithm")

                    if (!pk.isNullOrBlank() && !sigData.isNullOrBlank() && !algo.isNullOrBlank()) {
                        signatures.add(
                            Entry.Signature(
                                signerId = userId,
                                publicKey = pk,
                                signatureData = sigData,
                                algorithm = algo
                            )
                        )
                    }
                }
            }

            // Recipients
            val recipients = mutableListOf<String>()
            conn.prepareStatement("SELECT user_id FROM entry_recipients WHERE entry_id=?").use { ps2 ->
                ps2.setString(1, entryId)
                val rs2 = ps2.executeQuery()
                while (rs2.next()) recipients.add(rs2.getString("user_id"))
            }

            // Related entries
            val relatedEntries = mutableListOf<String>()
            conn.prepareStatement("SELECT related_entry_id FROM related_entries WHERE entry_id=?").use { ps2 ->
                ps2.setString(1, entryId)
                val rs2 = ps2.executeQuery()
                while (rs2.next()) relatedEntries.add(rs2.getString("related_entry_id"))
            }

            // page_num may be NULL
            var pageNum: Int? = null
            conn.prepareStatement("select page_num from page_entries where entry_id=?").use { ps2 ->
                ps2.setString(1, entryId)
                val rs2 = ps2.executeQuery()
                while (rs2.next()) pageNum = rs2.getInt("page_num")
            }

            Entry(
                id = rs.getString("id"),
                timestamp = rs.getLong("timestamp"),
                content = rs.getString("content"),
                senders = senders,
                recipients = recipients,
                hash = rs.getString("hash"),
                signatures = signatures,
                ledgerName = rs.getString("ledger_name"),
                pageNum = pageNum,
                relatedEntries = relatedEntries,
                keywords = keywords
            )
        }
    }


    override fun updateEntry(entry: Entry) {
        getConnection().use { conn ->
            // Update content and keywords
            conn.prepareStatement(
                "UPDATE entries SET content=?, keywords=? WHERE id=?"
            ).use { ps ->
                ps.setString(1, entry.content)
                ps.setObject(2, toJsonb(entry.keywords))
                ps.setString(3, entry.id)
                ps.executeUpdate()
            }

            // Add new signatures without removing existing ones
            entry.signatures.forEach { sig ->
                conn.prepareStatement(
                    "INSERT INTO entry_senders (entry_id, user_id, public_key, sig_data, algorithm) " +
                            "VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING"
                ).use { ps ->
                    ps.setString(1, entry.id)
                    ps.setString(2, sig.signerId)
                    ps.setString(3, sig.publicKey)
                    ps.setString(4, sig.signatureData)
                    ps.setString(5, sig.algorithm)
                    ps.executeUpdate()
                }
            }

            // Update related entries: replace completely for simplicity
            conn.prepareStatement("DELETE FROM related_entries WHERE entry_id=?").use { ps ->
                ps.setString(1, entry.id)
                ps.executeUpdate()
            }
            entry.relatedEntries.forEach { related ->
                conn.prepareStatement(
                    "INSERT INTO related_entries (entry_id, related_entry_id) VALUES (?,?)"
                ).use { ps ->
                    ps.setString(1, entry.id)
                    ps.setString(2, related)
                    ps.executeUpdate()
                }
            }
        }
    }

    private fun readEntriesForPage(conn: Connection, ledgerName: String, pageNumber: Int): List<Entry> {
        val entries = mutableListOf<Entry>()
        conn.prepareStatement(
            "SELECT entry_id FROM page_entries WHERE ledger_name=? AND page_num=? ORDER BY entry_id"
        ).use { ps ->
            ps.setString(1, ledgerName)
            ps.setInt(2, pageNumber)
            val rs = ps.executeQuery()
            while (rs.next()) {
                readEntry(rs.getString("entry_id"))?.let { entries.add(it) }
            }
        }
        return entries
    }
}
