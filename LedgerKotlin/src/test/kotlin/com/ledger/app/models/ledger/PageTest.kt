package com.ledger.app.models.ledger

import com.ledger.app.utils.hash.HashProvider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.*

class PageTest {

    private val hashAlgorithm = HashProvider.getSupportedAlgorithms().first()

    private fun buildEntry(id: String, content: String = "data"): Entry {
        return EntryBuilder(
            id = id,
            timestamp = 12345L,
            content = content,
            senders = listOf("Alice"),
            recipients = emptyList(),
            ledgerName = "LedgerX",
            hashAlgorithm = hashAlgorithm
        ).build()
    }

    @Test
    @DisplayName("Build page with single entry")
    fun testSingleEntryPage() {
        val entry = buildEntry("1")
        val page = PageBuilder(
            ledgerName = "LedgerX",
            number = 1,
            timestamp = 1000L,
            entries = listOf(entry),
            hashAlgorithm = hashAlgorithm
        ).build()

        assertEquals(entry.hash, page.merkleRoot, "Merkle root should equal the entry hash when only one entry")
        assertEquals(1, page.entries.size)
        assertNotNull(page.hash)
    }

    @Test
    @DisplayName("Build page with multiple entries")
    fun testMultipleEntriesPage() {
        val entries = listOf(buildEntry("1"), buildEntry("2"))
        val page = PageBuilder(
            ledgerName = "LedgerX",
            number = 2,
            timestamp = 2000L,
            entries = entries,
            hashAlgorithm = hashAlgorithm
        ).build()

        assertEquals(entries.size, page.entries.size)
        assertNotEquals(entries[0].hash, page.merkleRoot, "Merkle root should be combined for multiple entries")
    }

    @Test
    @DisplayName("Stable hash for identical pages")
    fun testStableHash() {
        val entries = listOf(buildEntry("1"))
        val page1 = PageBuilder(
            ledgerName = "LedgerX",
            number = 4,
            timestamp = 4000L,
            entries = entries,
            hashAlgorithm = hashAlgorithm
        ).build()
        val page2 = PageBuilder(
            ledgerName = "LedgerX",
            number = 4,
            timestamp = 4000L,
            entries = entries,
            hashAlgorithm = hashAlgorithm
        ).build()

        assertEquals(page1.hash, page2.hash, "Identical inputs should produce same hash")
    }

    @Test
    @DisplayName("Hash changes when entries change")
    fun testHashChangesOnEntriesChange() {
        val entry1 = buildEntry("1", "dataA")
        val entry2 = buildEntry("1", "dataB")

        val page1 = PageBuilder(
            ledgerName = "LedgerX",
            number = 5,
            timestamp = 5000L,
            entries = listOf(entry1),
            hashAlgorithm = hashAlgorithm
        ).build()
        val page2 = PageBuilder(
            ledgerName = "LedgerX",
            number = 5,
            timestamp = 5000L,
            entries = listOf(entry2),
            hashAlgorithm = hashAlgorithm
        ).build()

        assertNotEquals(page1.hash, page2.hash, "Page hash should change if entry content changes")
    }

    @Test
    @DisplayName("Hash changes when metadata changes")
    fun testHashChangesOnMetadataChange() {
        val entries = listOf(buildEntry("1"))

        val basePage = PageBuilder(
            ledgerName = "LedgerX",
            number = 6,
            timestamp = 6000L,
            entries = entries,
            hashAlgorithm = hashAlgorithm
        ).build()

        val differentNumber = PageBuilder(
            ledgerName = "LedgerX",
            number = 99,
            timestamp = 6000L,
            entries = entries,
            hashAlgorithm = hashAlgorithm
        ).build()
        assertNotEquals(basePage.hash, differentNumber.hash)

        val differentTimestamp = PageBuilder(
            ledgerName = "LedgerX",
            number = 6,
            timestamp = 9999L,
            entries = entries,
            hashAlgorithm = hashAlgorithm
        ).build()
        assertNotEquals(basePage.hash, differentTimestamp.hash)

        val differentPrevHash = PageBuilder(
            ledgerName = "LedgerX",
            number = 6,
            timestamp = 6000L,
            previousHash = "somePrevHash",
            entries = entries,
            hashAlgorithm = hashAlgorithm
        ).build()
        assertNotEquals(basePage.hash, differentPrevHash.hash)
    }

    @Test
    @DisplayName("Odd number of entries duplicates last entry in Merkle root computation")
    fun testOddNumberOfEntriesMerkleRoot() {
        val entries = listOf(buildEntry("1"), buildEntry("2"), buildEntry("3"))
        val merkleTree = PageBuilder.computeMerkleTree(entries, hashAlgorithm)
        val merkleRoot = merkleTree[merkleTree.size-1][0]

        // Expected: (h1|h2), (h3|h3) then combined
        val h1 = entries[0].hash
        val h2 = entries[1].hash
        val h3 = entries[2].hash
        val pair1 = HashProvider.toHexString(HashProvider.hash("$h1|$h2", hashAlgorithm))
        val pair2 = HashProvider.toHexString(HashProvider.hash("$h3|$h3", hashAlgorithm))
        val expectedRoot = HashProvider.toHexString(HashProvider.hash("$pair1|$pair2", hashAlgorithm))

        assertEquals(expectedRoot, merkleRoot)
    }

    @Test
    @DisplayName("Builder throws when required fields are missing")
    fun testBuilderIllegalStates() {
        assertFailsWith<IllegalStateException> {
            PageBuilder().build()
        }
        assertFailsWith<IllegalStateException> {
            PageBuilder().ledgerName("LedgerX").build()
        }
        assertFailsWith<IllegalStateException> {
            PageBuilder().ledgerName("LedgerX").number(1).build()
        }
        assertFailsWith<IllegalStateException> {
            PageBuilder().ledgerName("LedgerX").number(1).timestamp(123).build()
        }
        assertFailsWith<IllegalStateException> {
            PageBuilder().ledgerName("LedgerX").number(1).timestamp(123).entries(listOf(buildEntry("1"))).build()
        }
    }
}
