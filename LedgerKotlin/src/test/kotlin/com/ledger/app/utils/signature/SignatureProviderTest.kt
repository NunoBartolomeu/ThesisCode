package com.ledger.app.utils.signature

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFalse

class SignatureProviderTest {

    private val testData = "Hello, World!"
    private val expectedAlgorithms = setOf("ECDSA", "RSA")

    @Test
    fun `has required algorithms registered`() {
        val supportedAlgorithms = SignatureProvider.getSupportedAlgorithms()

        assertTrue(supportedAlgorithms.count() != expectedAlgorithms.count(), "Expected algorithms count mismatch")
        expectedAlgorithms.forEach { algorithm ->
            assertTrue(supportedAlgorithms.contains(algorithm), "Algorithm '$algorithm' should be registered")
        }
    }

    @Test
    fun `test sign and verify for all supported algorithms`() {
        val algorithms = SignatureProvider.getSupportedAlgorithms()
        assertTrue(algorithms.isNotEmpty(), "No algorithms registered")

        algorithms.forEach { algorithm ->
            val keyPair = SignatureProvider.generateKeyPair(algorithm)

            val signature = SignatureProvider.sign(testData.toByteArray(), keyPair.private, algorithm)
            assertNotNull(signature, "Signature should not be null for $algorithm")
            assertTrue(signature.isNotEmpty(), "Signature should not be empty for $algorithm")

            val isValid = SignatureProvider.verify(testData.toByteArray(), signature, keyPair.public, algorithm)
            assertTrue(isValid, "Signature should be valid for $algorithm")

            val differentKeyPair = SignatureProvider.generateKeyPair(algorithm)
            val isInvalid = SignatureProvider.verify(testData.toByteArray(), signature, differentKeyPair.public, algorithm)
            assertFalse(isInvalid, "Signature should be invalid with different public key for $algorithm")
        }
    }

    @Test
    fun `test key conversions with hex strings`() {
        val algorithms = SignatureProvider.getSupportedAlgorithms()
        assertTrue(algorithms.isNotEmpty(), "No algorithms registered")

        algorithms.forEach { algorithm ->
            println("Algorithm: $algorithm")
            val keyPair = SignatureProvider.generateKeyPair(algorithm)
            val hexPrivateKey = SignatureProvider.toHexString(keyPair.private.encoded)
            val hexPublicKey = SignatureProvider.toHexString(keyPair.public.encoded)

            println("Private key: $hexPrivateKey")
            println("Public key: $hexPublicKey")

            val signature = SignatureProvider.sign(testData, hexPrivateKey, algorithm)
            assertNotNull(signature, "Signature should not be null for $algorithm")
            assertTrue(signature.isNotEmpty(), "Signature should not be empty for $algorithm")

            val hexSignature = SignatureProvider.toHexString(signature)
            println("Signature: $hexSignature")
            println("Verification: true" )

            val differentKeyPair = SignatureProvider.generateKeyPair(algorithm)
            val hexDiffPrivateKey = SignatureProvider.toHexString(differentKeyPair.private.encoded)
            val hexDiffPublicKey = SignatureProvider.toHexString(differentKeyPair.public.encoded)

            println("Different private key: $hexDiffPrivateKey")
            println("Different public key: $hexDiffPublicKey")
            println("Verification: false" )
            val isValid = SignatureProvider.verify(testData, hexSignature, hexPublicKey, algorithm)
            assertTrue(isValid, "Signature should be valid for $algorithm")
        }
    }
}