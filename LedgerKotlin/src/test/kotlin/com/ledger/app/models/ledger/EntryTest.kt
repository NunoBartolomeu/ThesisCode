package com.ledger.app.models.ledger

import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.*

class EntryTest {
    private val hashAlgorithm = HashProvider.getSupportedAlgorithms().first()
    private val sigAlgorithm = SignatureProvider.getSupportedAlgorithms().first()

    private fun buildBasicEntry(
        id: String = "1",
        timestamp: Long = System.currentTimeMillis(),
        content: String = "TestContent",
        senders: List<String> = listOf("Alice"),
        recipients: List<String> = emptyList()
    ): Entry {
        return EntryBuilder(
            id = id,
            timestamp = timestamp,
            content = content,
            senders = senders,
            recipients = recipients,
            ledgerName = "LedgerTest",
            hashAlgorithm = hashAlgorithm
        ).build()
    }

    @Test
    @DisplayName("Create 1 sender, 0 receiver entry")
    fun testSingleSenderEntry() {
        val entry = buildBasicEntry()
        assertEquals(1, entry.senders.size)
        assertEquals(0, entry.recipients.size)
        assertNotNull(entry.hash)
    }

    @Test
    @DisplayName("Create 2 sender, 1 receiver entry and test progressive signing")
    fun testTwoSenderOneReceiverSigning() {
        val senders = listOf("Alice", "Bob")
        val entry = buildBasicEntry(
            id = "2",
            senders = senders,
            recipients = listOf("Charlie")
        )
        assertEquals(2, entry.senders.size)
        assertEquals(1, entry.recipients.size)

        // No signatures
        assertFalse(entry.verify(), "Should not be fully signed with no signatures")

        // Sign with Alice only
        val keyPairAlice = SignatureProvider.generateKeyPair(sigAlgorithm)
        val sigAlice = Entry.Signature(
            signerId = "Alice",
            publicKey = SignatureProvider.keyOrSigToString(keyPairAlice.public.encoded),
            signatureData = SignatureProvider.keyOrSigToString(
                SignatureProvider.sign(entry.hash, keyPairAlice.private, sigAlgorithm)
            ),
            algorithm = sigAlgorithm
        )
        val entryWithAlice = entry.copy(signatures = listOf(sigAlice))
        assertFalse(entryWithAlice.verify(), "Only Alice signed, should not be fully signed")

        // Add Bob’s signature
        val keyPairBob = SignatureProvider.generateKeyPair(sigAlgorithm)
        val sigBob = Entry.Signature(
            signerId = "Bob",
            publicKey = SignatureProvider.keyOrSigToString(keyPairBob.public.encoded),
            signatureData = SignatureProvider.keyOrSigToString(
                SignatureProvider.sign(entry.hash, keyPairBob.private, sigAlgorithm)
            ),
            algorithm = sigAlgorithm
        )
        val entryWithBoth = entry.copy(signatures = listOf(sigAlice, sigBob))
        assertTrue(entryWithBoth.verify(), "All senders signed, should be fully signed")
    }

    @Test
    @DisplayName("Stable hash for identical entries")
    fun testStableHash() {
        val entry1 = buildBasicEntry(id = "4", timestamp = 12345L)
        val entry2 = buildBasicEntry(id = "4", timestamp = 12345L)
        assertEquals(entry1.hash, entry2.hash, "Entries with same inputs should have same hash")
    }

    @Test
    @DisplayName("Hash changes with different timestamps")
    fun testTimestampSensitivity() {
        val entry1 = buildBasicEntry(id = "5", timestamp = 11111L)
        val entry2 = buildBasicEntry(id = "5", timestamp = 22222L)
        assertNotEquals(entry1.hash, entry2.hash, "Different timestamps should produce different hashes")
    }

    @Test
    @DisplayName("Builder throws on missing required fields")
    fun testBuilderIllegalStates() {
        assertFailsWith<IllegalStateException> {
            EntryBuilder().build()
        }
        assertFailsWith<IllegalStateException> {
            EntryBuilder().id("x").build()
        }
        assertFailsWith<IllegalStateException> {
            EntryBuilder().id("x").timestamp(123).build()
        }
        assertFailsWith<IllegalStateException> {
            EntryBuilder().id("x").timestamp(123).content("abc").build()
        }
        assertFailsWith<IllegalStateException> {
            EntryBuilder().id("x").timestamp(123).content("abc").ledgerName("LedgerX").build()
        }
    }

    @Test
    @DisplayName("False signature fails verification")
    fun testFalseSignature() {
        val entry = buildBasicEntry(id = "5")
        val keyPair = SignatureProvider.generateKeyPair(sigAlgorithm)

        val badSignature = SignatureProvider.sign("NotTheHash", keyPair.private, sigAlgorithm)
        val sig = Entry.Signature(
            signerId = "Alice",
            publicKey = SignatureProvider.keyOrSigToString(keyPair.public.encoded),
            signatureData = SignatureProvider.keyOrSigToString(badSignature),
            algorithm = sigAlgorithm
        )

        val signedEntry = entry.copy(signatures = listOf(sig))
        assertFalse(signedEntry.verify(), "Verification should fail with false signature")
    }
}
