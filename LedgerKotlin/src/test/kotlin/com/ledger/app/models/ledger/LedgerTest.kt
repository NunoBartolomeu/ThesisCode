package com.ledger.app.models.ledger

import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.*

class LedgerTest {
    private val content = "Hello, World!"
    private val hashAlgorithm = HashProvider.getDefaultAlgorithm()
    private val sigAlgorithm = SignatureProvider.getDefaultAlgorithm()
    private val ledgerConfig = LedgerConfig("test-ledger", entriesPerPage = 2, hashAlgorithm = hashAlgorithm)
    private lateinit var ledger: Ledger

    private val senderId = "Alice"
    private val sender2Id = "Bob"
    private val recipientId = "Charlie"

    private val keyPair1 = SignatureProvider.generateKeyPair(sigAlgorithm)
    private val keyPair2 = SignatureProvider.generateKeyPair(sigAlgorithm)

    @BeforeTest
    fun setup() {
        ledger = Ledger(ledgerConfig)
    }

    private fun signEntry(entry: Entry, keyPair: java.security.KeyPair, signerId: String): Entry.Signature {
        val sigBytes = SignatureProvider.sign(entry.hash, keyPair.private, sigAlgorithm)
        val sigHex = SignatureProvider.keyOrSigToString(sigBytes)
        val pubHex = SignatureProvider.keyOrSigToString(keyPair.public.encoded)
        return Entry.Signature(
            signerId = signerId,
            publicKey = pubHex,
            signatureData = sigHex,
            algorithm = sigAlgorithm
        )
    }

    @Test
    @DisplayName("create entry with 1 sender no recipient")
    fun createSimpleEntry() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        assertTrue(ledger.holdingArea.contains(entry))
        assertEquals(senderId, entry.senders.first())
        assertTrue(entry.recipients.isEmpty())
    }

    @Test
    @DisplayName("add valid signature")
    fun addSignature() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        val sig = signEntry(entry, keyPair1, senderId)

        val updated = ledger.addSignature(entry.id, sig)
        assertTrue(updated.signatures.isNotEmpty())
        assertFalse(ledger.holdingArea.contains(updated))
        assertTrue(ledger.verifiedEntries.contains(updated))
    }

    @Test
    @DisplayName("duplicate signature ignored")
    fun blockDuplicateSignatures() {
        val entry = ledger.createEntry(content, listOf(senderId, sender2Id), emptyList(), emptyList(), emptyList())
        val sig = signEntry(entry, keyPair1, senderId)

        val updated1 = ledger.addSignature(entry.id, sig)
        val updated2 = ledger.addSignature(entry.id, sig)

        assertEquals(1, updated2.signatures.size)
    }

    @Test
    @DisplayName("invalid signature throws exception")
    fun addInvalidSignature() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        val badSig = Entry.Signature(
            signerId = senderId,
            publicKey = SignatureProvider.keyOrSigToString(keyPair1.public.encoded),
            signatureData = "WrongSignature",
            algorithm = sigAlgorithm
        )

        assertFailsWith<Exception> { ledger.addSignature(entry.id, badSig) }
    }

    @Test
    @DisplayName("non sender signature throws exception")
    fun addSignatureFromNonSender() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        val sig = signEntry(entry, keyPair1, "Dave")

        assertFailsWith<Exception> { ledger.addSignature(entry.id, sig) }
    }

    @Test
    @DisplayName("verify entry flow")
    fun addAllSignatures() {
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
    @DisplayName("update entry between holding and verified")
    fun addAllSignaturesAndCheckState() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        assertTrue(ledger.holdingArea.contains(entry))

        val signed = entry.copy(signatures = listOf(signEntry(entry, keyPair1, senderId)))
        ledger.updateEntry(signed)

        assertFalse(ledger.holdingArea.contains(signed))
        assertTrue(ledger.verifiedEntries.contains(signed))
    }

    @Test
    @DisplayName("page created when threshold reached")
    fun checkPageAutoCreation() {
        val e1 = ledger.createEntry("c1", listOf(senderId), emptyList(), emptyList(), emptyList())
        val e2 = ledger.createEntry("c2", listOf(senderId), emptyList(), emptyList(), emptyList())

        val s1 = signEntry(e1, keyPair1, senderId)
        val s2 = signEntry(e2, keyPair1, senderId)

        ledger.addSignature(e1.id, s1)
        ledger.addSignature(e2.id, s2)

        assertTrue(ledger.pages.isNotEmpty())
        assertEquals(2, ledger.pages.first().entries.size)
        assertTrue(ledger.verifiedEntries.isEmpty())
    }

    @Test
    @DisplayName("page previous hash check")
    fun checkPreviousHash() {
        val e1 = ledger.createEntry("c1", listOf(senderId), emptyList(), emptyList(), emptyList())
        val e2 = ledger.createEntry("c2", listOf(senderId), emptyList(), emptyList(), emptyList())
        val e3 = ledger.createEntry("c3", listOf(senderId), emptyList(), emptyList(), emptyList())
        val e4 = ledger.createEntry("c4", listOf(senderId), emptyList(), emptyList(), emptyList())

        ledger.addSignature(e1.id, signEntry(e1, keyPair1, senderId))
        ledger.addSignature(e2.id, signEntry(e2, keyPair1, senderId))
        ledger.addSignature(e3.id, signEntry(e3, keyPair1, senderId))
        ledger.addSignature(e4.id, signEntry(e4, keyPair1, senderId))

        assertEquals(2, ledger.pages.size)
        val first = ledger.pages[0]
        val second = ledger.pages[1]
        assertEquals(first.hash, second.previousHash)
    }

    @Test
    @DisplayName("inclusion proof success and failure")
    fun checkInclusionProof() {
        val e1 = ledger.createEntry("c1", listOf(senderId), emptyList(), emptyList(), emptyList())
        val e2 = ledger.createEntry("c2", listOf(senderId), emptyList(), emptyList(), emptyList())
        ledger.addSignature(e1.id, signEntry(e1, keyPair1, senderId))
        ledger.addSignature(e2.id, signEntry(e2, keyPair1, senderId))

        val page = ledger.pages.first()
        val proof = ledger.getInclusionProof(page.entries.first())
        assertTrue(proof.isNotEmpty())

        val fakeEntry = e1.copy(id = "nonexistent")
        val badProof = ledger.getInclusionProof(fakeEntry)
        assertTrue(badProof.isEmpty())
    }

    @Test
    @DisplayName("get entry by id works")
    fun getEntry() {
        val entry = ledger.createEntry("c1", listOf(senderId), emptyList(), emptyList(), emptyList())
        assertEquals(entry, ledger.getEntryById(entry.id))
        assertNull(ledger.getEntryById("random-id"))
    }

    @Test
    @DisplayName("erase entry content produces correct deleted format and hashes")
    fun eraseEntryContentTest() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        val signed = ledger.addSignature(entry.id, signEntry(entry, keyPair1, senderId))

        val erased = ledger.eraseEntryContent(signed.id, senderId)

        assertTrue(erased.isDeleted())
        assertTrue(erased.content.startsWith("DELETED_ENTRY"))

        val storedContentHash = Regex("content_hash:([^\n]+)").find(erased.content)?.groupValues?.get(1)
        val storedEntryHash = Regex("entry_hash:([^\n]+)").find(erased.content)?.groupValues?.get(1)

        val expectedContentHash = HashProvider.toHashString(HashProvider.hash(content, hashAlgorithm))
        assertEquals(expectedContentHash, storedContentHash)
        assertEquals(signed.hash, storedEntryHash)
    }

    @Test
    @DisplayName("restore entry content works and validates hash")
    fun restoreEntryContentTest() {
        val entry = ledger.createEntry(content, listOf(senderId), emptyList(), emptyList(), emptyList())
        val signed = ledger.addSignature(entry.id, signEntry(entry, keyPair1, senderId))
        val erased = ledger.eraseEntryContent(entry.id, senderId)

        val restored = ledger.restoreEntryContent(entry.id, senderId, content)
        assertEquals(content, restored.content)
        assertEquals(signed.hash, restored.hash)

        assertFailsWith<Exception> {
            ledger.restoreEntryContent(erased.id, senderId, "Tampered content")
        }
    }
}
