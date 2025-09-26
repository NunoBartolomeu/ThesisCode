package com.ledger.app.models.ledger

import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider
import java.security.KeyPair

data class Receipt(
    val entry: Entry,
    val timestamp: Long,
    val requesterId: String,
    val proof: List<String>,

    val hash: String,
    val hashAlgorithm: String,

    val signature: Entry.Signature,
)

class ReceiptBuilder() {
    private var entry: Entry? = null
    private var timestamp: Long? = null
    private var requesterId: String? = null
    private val proof = mutableListOf<String>()
    private var systemKeyPair: KeyPair? = null
    private var signatureAlgorithm: String? = null
    private var hashAlgorithm: String? = null

    constructor(
        entry: Entry,
        requesterId: String,
        proof: List<String> = emptyList(),
        systemKeyPair: KeyPair,
        signatureAlgorithm: String,
        hashAlgorithm: String,
        timestamp: Long,
    ) : this() {
        this.entry = entry
        this.timestamp = timestamp
        this.requesterId = requesterId
        this.proof.addAll(proof)
        this.systemKeyPair = systemKeyPair
        this.hashAlgorithm = hashAlgorithm
        this.signatureAlgorithm = signatureAlgorithm
    }

    fun entry(entry: Entry) = apply { this.entry = entry }

    fun timestamp(timestamp: Long) = apply { this.timestamp = timestamp }

    fun requesterId(requesterId: String) = apply { this.requesterId = requesterId }

    fun proof(proof: List<String>) = apply {
        this.proof.clear()
        this.proof.addAll(proof)
    }

    fun addProofHash(hash: String) = apply { this.proof.add(hash) }

    fun systemKeyPair(keyPair: KeyPair) = apply { this.systemKeyPair = keyPair }

    fun signatureAlgorithm(algorithm: String) = apply { this.signatureAlgorithm = algorithm }

    companion object {
        fun computeHash(
            entryHash: String,
            timestamp: Long,
            requesterId: String,
            proof: List<String>,
            hashAlgorithm: String
        ): String {
            val receiptData = listOf(
                entryHash,
                timestamp.toString(),
                requesterId,
                proof.joinToString(",")
            ).joinToString("|")

            return HashProvider.toHexString(HashProvider.hash(receiptData, hashAlgorithm))
        }
    }

    fun build(): Receipt {
        val receiptEntry = entry ?: throw IllegalStateException("Entry is required")
        val receiptTimestamp = timestamp ?: throw IllegalStateException("Timestamp is required")
        val receiptRequesterId = requesterId ?: throw IllegalStateException("Requester ID is required")
        val keyPair = systemKeyPair ?: throw IllegalStateException("System key pair is required")
        val sigAlgorithm = signatureAlgorithm ?: throw IllegalStateException("Signature algorithm is required")
        val hashAlgorithm = hashAlgorithm ?: throw IllegalStateException("Signature algorithm is required")

        val receiptHash = computeHash(
            entryHash = receiptEntry.hash,
            timestamp = receiptTimestamp,
            requesterId = receiptRequesterId,
            proof = proof,
            hashAlgorithm = hashAlgorithm
        )

        val signatureBytes = SignatureProvider.sign(
            receiptHash.toByteArray(),
            keyPair.private,
            sigAlgorithm
        )

        return Receipt(
            entry = receiptEntry,
            timestamp = receiptTimestamp,
            requesterId = receiptRequesterId,
            proof = proof.toList(),
            hash = receiptHash,
            signature = Entry.Signature(
                "Ledger System",
                SignatureProvider.toHexString(keyPair.public.encoded),
                SignatureProvider.toHexString(signatureBytes),
                sigAlgorithm
            ),
            hashAlgorithm = hashAlgorithm
        )
    }
}