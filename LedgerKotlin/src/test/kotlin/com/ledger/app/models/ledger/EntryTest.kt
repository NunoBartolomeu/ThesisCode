package com.ledger.app.models.ledger

import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.*

class EntryTest {
    private val hashAlgorithm: String= HashProvider.getSupportedAlgorithms().first()
    private val sigAlgorithms: Set<String> = SignatureProvider.getSupportedAlgorithms()

    private fun buildBasicEntry(
        id: String = "1",
        timestamp: Long = System.currentTimeMillis(),
        content: String = "TestContent",
        senders: List<String> = listOf("Alice"),
        recipients: List<String> = emptyList(),
        relatedEntries: List<String> = emptyList(),
        keywords: List<String> = emptyList()
    ): Entry {
        return EntryBuilder(
            id = id,
            timestamp = timestamp,
            content = content,
            senders = senders,
            recipients = recipients,
            ledgerName = "LedgerTest",
            hashAlgorithm = hashAlgorithm
        ).build().copy(
            relatedEntries = relatedEntries,
            keywords = keywords
        )
    }

    @Nested
    inner class CreationPhase {

        @Test
        fun `create entry with single sender and no recipients`() {
            val entry = buildBasicEntry()
            assertEquals(1, entry.senders.size)
            assertEquals(0, entry.recipients.size)
            assertNotNull(entry.hash)
        }

        @Test
        fun `create entry with multiple senders`() {
            val entry = buildBasicEntry(senders = List(100) { "Sender$it" })
            assertEquals(100, entry.senders.size)
        }

        @Test
        fun `create entry with multiple recipients`() {
            val entry = buildBasicEntry(recipients = List(100) { "Recipient$it" })
            assertEquals(100, entry.recipients.size)
        }

        @Test
        fun `create entry with no senders`() {
        }

        @Test
        fun `create entry with zero recipients`() {
            val entry = buildBasicEntry(recipients = emptyList())
            assertEquals(0, entry.recipients.size)
        }

        @Test
        fun `hash is stable for identical inputs`() {
            val entry1 = buildBasicEntry(id = "same", timestamp = 12345L)
            val entry2 = buildBasicEntry(id = "same", timestamp = 12345L)
            assertEquals(entry1.hash, entry2.hash)
        }

        @Test
        fun `hash changes when timestamp changes`() {
            val entry1 = buildBasicEntry(id = "same", timestamp = 11111L)
            val entry2 = buildBasicEntry(id = "same", timestamp = 22222L)
            assertNotEquals(entry1.hash, entry2.hash)
        }

        @Test
        fun `timestamp is after a given threshold`() {
            val threshold = System.currentTimeMillis() - 1000
            val entry = buildBasicEntry()
            assertTrue(entry.timestamp > threshold)
        }

        @Test
        fun `builder throws when required fields are missing`() {
            assertFailsWith<IllegalStateException> { EntryBuilder().build() }
            assertFailsWith<IllegalStateException> { EntryBuilder().id("x").build() }
            assertFailsWith<IllegalStateException> { EntryBuilder().id("x").timestamp(123).build() }
            assertFailsWith<IllegalStateException> { EntryBuilder().id("x").timestamp(123).content("abc").build() }
            assertFailsWith<IllegalStateException> {
                EntryBuilder().id("x").timestamp(123).content("abc").ledgerName("LedgerX").build()
            }
        }

        @Test
        fun `add and remove related entries`() {
            val entry = buildBasicEntry(relatedEntries = listOf("r1", "r2"))
            assertEquals(listOf("r1", "r2"), entry.relatedEntries)

            val modified = entry.copy(relatedEntries = entry.relatedEntries - "r1")
            assertEquals(listOf("r2"), modified.relatedEntries)

            val unchanged = entry.copy(relatedEntries = entry.relatedEntries - "notExisting")
            assertEquals(listOf("r1", "r2"), unchanged.relatedEntries)
        }

        @Test
        fun `add and remove keywords`() {
            val entry = buildBasicEntry(keywords = listOf("k1", "k2"))
            assertEquals(listOf("k1", "k2"), entry.keywords)

            val modified = entry.copy(keywords = entry.keywords + "k3")
            assertTrue("k3" in modified.keywords)

            val removed = modified.copy(keywords = modified.keywords - "k1")
            assertFalse("k1" in removed.keywords)
        }
    }

    @Nested
    inner class SignaturePhase {

        @Test
        fun `entry without signatures does not verify`() {
            val entry = buildBasicEntry(senders = listOf("Alice", "Bob"))
            assertFalse(entry.verify())
        }

        @Test
        fun `entry verifies when all senders have signed`() {
            val senders = listOf("Alice", "Bob")
            val entry = buildBasicEntry(senders = senders)

            val aliceKey = SignatureProvider.generateKeyPair(sigAlgorithms.first())
            val bobKey = SignatureProvider.generateKeyPair(sigAlgorithms.first())

            val sigAlice = Entry.Signature(
                signerId = "Alice",
                publicKey = SignatureProvider.toHexString(aliceKey.public.encoded),
                signatureData = SignatureProvider.toHexString(
                    SignatureProvider.sign(entry.hash.toByteArray(), aliceKey.private, sigAlgorithms.first())
                ),
                algorithm = sigAlgorithms.first()
            )
            val sigBob = Entry.Signature(
                signerId = "Bob",
                publicKey = SignatureProvider.toHexString(bobKey.public.encoded),
                signatureData = SignatureProvider.toHexString(
                    SignatureProvider.sign(entry.hash.toByteArray(), bobKey.private, sigAlgorithms.first())
                ),
                algorithm = sigAlgorithms.first()
            )

            val signedEntry = entry.copy(signatures = listOf(sigAlice, sigBob))
            assertTrue(signedEntry.verify())
        }

        @Test
        fun `entry fails verification with incorrect signature`() {
            val entry = buildBasicEntry()
            val keyPair = SignatureProvider.generateKeyPair(sigAlgorithms.first())

            val badSig = SignatureProvider.sign("NotTheHash".toByteArray(), keyPair.private, sigAlgorithms.first())
            val sig = Entry.Signature(
                signerId = "Alice",
                publicKey = SignatureProvider.toHexString(keyPair.public.encoded),
                signatureData = SignatureProvider.toHexString(badSig),
                algorithm = sigAlgorithms.first()
            )

            val signedEntry = entry.copy(signatures = listOf(sig))
            assertFalse(signedEntry.verify())
        }

        @Test
        fun `entry supports multiple algorithms for different signers`() {
            val senders = listOf("Alice", "Bob")
            val entry = buildBasicEntry(senders = senders)

            val aliceAlg = sigAlgorithms.first()
            val bobAlg = sigAlgorithms.last()

            val aliceKey = SignatureProvider.generateKeyPair(aliceAlg)
            val bobKey = SignatureProvider.generateKeyPair(bobAlg)

            val sigAlice = Entry.Signature(
                signerId = "Alice",
                publicKey = SignatureProvider.toHexString(aliceKey.public.encoded),
                signatureData = SignatureProvider.toHexString(
                    SignatureProvider.sign(entry.hash.toByteArray(), aliceKey.private, aliceAlg)
                ),
                algorithm = aliceAlg
            )
            val sigBob = Entry.Signature(
                signerId = "Bob",
                publicKey = SignatureProvider.toHexString(bobKey.public.encoded),
                signatureData = SignatureProvider.toHexString(
                    SignatureProvider.sign(entry.hash.toByteArray(), bobKey.private, bobAlg)
                ),
                algorithm = bobAlg
            )

            val signedEntry = entry.copy(signatures = listOf(sigAlice, sigBob))
            assertTrue(signedEntry.verify())
        }

        @Test
        fun `entry does not verify when only one of many senders has signed`() {
            val senders = listOf("Alice", "Bob", "Charlie")
            val entry = buildBasicEntry(senders = senders)

            val keyPair = SignatureProvider.generateKeyPair(sigAlgorithms.first())
            val sigAlice = Entry.Signature(
                signerId = "Alice",
                publicKey = SignatureProvider.toHexString(keyPair.public.encoded),
                signatureData = SignatureProvider.toHexString(
                    SignatureProvider.sign(entry.hash.toByteArray(), keyPair.private, sigAlgorithms.first())
                ),
                algorithm = sigAlgorithms.first()
            )

            val signedEntry = entry.copy(signatures = listOf(sigAlice))
            assertFalse(signedEntry.verify())
        }
    }
}
