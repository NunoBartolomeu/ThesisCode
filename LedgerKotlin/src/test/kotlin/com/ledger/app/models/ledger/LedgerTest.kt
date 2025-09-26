package com.ledger.app.models.ledger

import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class LedgerTest {
    private val content = "Hello, World!"
    private val hashAlgorithm = HashProvider.getSupportedAlgorithms().toList()[2]
    private val sigAlgorithm = SignatureProvider.getSupportedAlgorithms().toList()[1]
    private val ledgerConfig = LedgerConfig("test-ledger", entriesPerPage = 2, hashAlgorithm = hashAlgorithm)
    private lateinit var ledger: Ledger

    private val senderId = "Alice"
    private val sender2Id = "Bob"
    private val recipientId = "Charlie"

    private val keyPair1 = SignatureProvider.generateKeyPair(sigAlgorithm)
    private val keyPair2 = SignatureProvider.generateKeyPair(sigAlgorithm)

    @BeforeEach
    fun setup() {
        ledger = Ledger(ledgerConfig)
    }

    private fun signEntry(entry: Entry, keyPair: java.security.KeyPair, signerId: String): Entry.Signature {
        val sigBytes = SignatureProvider.sign(entry.hash.toByteArray(), keyPair.private, sigAlgorithm)
        val sigHex = SignatureProvider.toHexString(sigBytes)
        val pubHex = SignatureProvider.toHexString(keyPair.public.encoded)
        return Entry.Signature(
            signerId = signerId,
            publicKey = pubHex,
            signatureData = sigHex,
            algorithm = sigAlgorithm
        )
    }

    @Test
    fun `create entry with one sender and no recipient`() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        assertTrue(ledger.unverifiedEntries.contains(entry))
        assertEquals(senderId, entry.senders.first())
        assertTrue(entry.recipients.isEmpty())
    }

    @Test
    fun `add valid signature`() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        val sig = signEntry(entry, keyPair1, senderId)

        val updated = ledger.addSignature(entry.id, sig)
        assertTrue(updated.signatures.isNotEmpty())
        assertFalse(ledger.unverifiedEntries.contains(updated))
        assertTrue(ledger.verifiedEntries.contains(updated))
    }

    @Test
    fun `reject duplicate signatures`() {
        val entry = ledger.createEntry(content, listOf(senderId, sender2Id), emptyList(), emptyList(), emptyList())
        val sig = signEntry(entry, keyPair1, senderId)

        ledger.addSignature(entry.id, sig)
        assertFailsWith<Exception> { ledger.addSignature(entry.id, sig) }
    }

    @Test
    fun `reject invalid signature`() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        val badSig = Entry.Signature(
            signerId = senderId,
            publicKey = SignatureProvider.toHexString(keyPair1.public.encoded),
            signatureData = "WrongSignature",
            algorithm = sigAlgorithm
        )
        assertFailsWith<Exception> { ledger.addSignature(entry.id, badSig) }
    }

    @Test
    fun `reject signature from non sender`() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        val sig = signEntry(entry, keyPair1, "Dave")
        assertFailsWith<Exception> { ledger.addSignature(entry.id, sig) }
    }

    @Test
    fun `add all signatures and verify entry`() {
        val entry = ledger.createEntry(content, listOf(senderId, sender2Id), listOf(recipientId), emptyList(), emptyList())
        assertFalse(entry.verify())

        val sig1 = signEntry(entry, keyPair1, senderId)
        val partial = ledger.addSignature(entry.id, sig1)
        assertFalse(partial.verify())

        val sig2 = signEntry(partial, keyPair2, sender2Id)
        val full = ledger.addSignature(entry.id, sig2)
        assertTrue(full.verify())
    }

    @Test
    fun `auto create page when threshold reached`() {
        val e1 = ledger.createEntry("c1", listOf(senderId), emptyList(), emptyList(), emptyList())
        val e2 = ledger.createEntry("c2", listOf(senderId), emptyList(), emptyList(), emptyList())

        ledger.addSignature(e1.id, signEntry(e1, keyPair1, senderId))
        ledger.addSignature(e2.id, signEntry(e2, keyPair1, senderId))

        assertTrue(ledger.pages.isNotEmpty())
        assertEquals(2, ledger.pages.first().entries.size)
        assertTrue(ledger.verifiedEntries.isEmpty())
    }

    @Test
    fun `page stores previous hash correctly`() {
        val e1 = ledger.createEntry("c1", listOf(senderId), emptyList(), emptyList(), emptyList())
        val e2 = ledger.createEntry("c2", listOf(senderId), emptyList(), emptyList(), emptyList())
        val e3 = ledger.createEntry("c3", listOf(senderId), emptyList(), emptyList(), emptyList())
        val e4 = ledger.createEntry("c4", listOf(senderId), emptyList(), emptyList(), emptyList())

        ledger.addSignature(e1.id, signEntry(e1, keyPair1, senderId))
        ledger.addSignature(e2.id, signEntry(e2, keyPair1, senderId))
        ledger.addSignature(e3.id, signEntry(e3, keyPair1, senderId))
        ledger.addSignature(e4.id, signEntry(e4, keyPair1, senderId))

        assertEquals(2, ledger.pages.size)
        assertEquals(ledger.pages.toList()[0].hash, ledger.pages.toList()[1].previousHash)
    }

    @Test
    fun `get inclusion proof succeeds`() {
        val e1 = ledger.createEntry("c1", listOf(senderId), emptyList(), emptyList(), emptyList())
        val e2 = ledger.createEntry("c2", listOf(senderId), emptyList(), emptyList(), emptyList())
        ledger.addSignature(e1.id, signEntry(e1, keyPair1, senderId))
        ledger.addSignature(e2.id, signEntry(e2, keyPair1, senderId))

        val page = ledger.pages.first()
        val proof = ledger.getInclusionProof(page.entries.first())
        assertTrue(proof.isNotEmpty())
    }


    @Test
    fun `get inclusion proof fails`() {
        val e1 = ledger.createEntry("c1", listOf(senderId), emptyList(), emptyList(), emptyList())
        val e2 = ledger.createEntry("c2", listOf(senderId), emptyList(), emptyList(), emptyList())
        ledger.addSignature(e1.id, signEntry(e1, keyPair1, senderId))
        ledger.addSignature(e2.id, signEntry(e2, keyPair1, senderId))

        val page = ledger.pages.first()
        val proof = ledger.getInclusionProof(page.entries.first())
        println(proof)

        val fakeEntry = e1.copy(id = "nonexistent")
        assertFailsWith<Exception> { ledger.getInclusionProof(fakeEntry) }
    }

    @Test
    fun `get entry by id`() {
        val entry = ledger.createEntry("c1", listOf(senderId), emptyList(), emptyList(), emptyList())
        assertEquals(entry, ledger.getEntryById(entry.id))
        assertNull(ledger.getEntryById("random-id"))
    }

    @Test
    fun `erase entry content`() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        val signed = ledger.addSignature(entry.id, signEntry(entry, keyPair1, senderId))

        val erased = ledger.eraseEntryContent(signed.id, senderId)
        assertTrue(erased.isDeleted())
        assertTrue(erased.content.startsWith("DELETED_ENTRY"))
    }

    @Test
    fun `restore erased entry content`() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        val signed = ledger.addSignature(entry.id, signEntry(entry, keyPair1, senderId))
        ledger.eraseEntryContent(entry.id, senderId)

        val restored = ledger.restoreEntryContent(entry.id, senderId, content)
        assertEquals(content, restored.content)
        assertEquals(signed.hash, restored.hash)

        assertFailsWith<Exception> {
            ledger.restoreEntryContent(restored.id, senderId, "Tampered")
        }
    }

    @Test
    fun `create receipt for valid participant`() {
        val entry = ledger.createEntry(content, listOf(senderId), listOf(recipientId), emptyList(), emptyList())
        ledger.addSignature(entry.id, signEntry(entry, keyPair1, senderId))

        val receipt = ledger.createReceipt(entry.id, senderId, keyPair1, sigAlgorithm)
        assertEquals(entry.id, receipt.entry.id)
        assertTrue(receipt.proof.isEmpty() || receipt.proof.all { it.isNotBlank() })
        assertEquals(senderId, receipt.requesterId)
    }

    @Test
    fun `reject receipt request from non participant`() {
        val entry = ledger.createEntry(content, listOf(senderId), listOf(recipientId), emptyList(), emptyList())
        ledger.addSignature(entry.id, signEntry(entry, keyPair1, senderId))

        assertFailsWith<Exception> { ledger.createReceipt(entry.id, "Mallory", keyPair1, sigAlgorithm) }
    }

    @Test
    fun `reject receipt creation for non existing entry`() {
        assertFailsWith<Exception> { ledger.createReceipt("fake-id", senderId, keyPair1, sigAlgorithm) }
    }
}
