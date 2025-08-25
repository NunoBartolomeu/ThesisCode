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
    @DisplayName("Has required algorithms registered")
    fun hasRequiredAlgorithms() {
        val supportedAlgorithms = SignatureProvider.getSupportedAlgorithms()

        assertTrue(
            supportedAlgorithms.count() >= expectedAlgorithms.count(),
            "Missing required algorithms"
        )

        supportedAlgorithms.forEach {
            println("$it is a supported signature algorithm")
        }

        expectedAlgorithms.forEach { algorithm ->
            assertTrue(
                supportedAlgorithms.contains(algorithm),
                "Algorithm '$algorithm' should be registered"
            )
        }
    }

    @Test
    @DisplayName("Test sign and verify for all supported algorithms")
    fun testSignAndVerifyAllAlgorithms() {
        val algorithms = SignatureProvider.getSupportedAlgorithms()
        assertTrue(algorithms.isNotEmpty(), "No algorithms registered")

        algorithms.forEach { algorithm ->
            println("Testing algorithm: $algorithm")

            val keyPair = SignatureProvider.generateKeyPair(algorithm)

            val signature = SignatureProvider.sign(testData, keyPair.private, algorithm)
            assertNotNull(signature, "Signature should not be null for $algorithm")
            assertTrue(signature.isNotEmpty(), "Signature should not be empty for $algorithm")

            val isValid = SignatureProvider.verify(testData, signature, keyPair.public, algorithm)
            assertTrue(isValid, "Signature should be valid for $algorithm")

            val differentKeyPair = SignatureProvider.generateKeyPair(algorithm)
            val isInvalid = SignatureProvider.verify(testData, signature, differentKeyPair.public, algorithm)
            assertFalse(isInvalid, "Signature should be invalid with different public key for $algorithm")
        }
    }

    @Test
    @DisplayName("Test hex string conversions")
    fun testHexConversions() {
        val algorithm = "ECDSA"
        val keyPair = SignatureProvider.generateKeyPair(algorithm)

        val privateKeyBytes = keyPair.private.encoded
        val publicKeyBytes = keyPair.public.encoded

        val privateKeyHex = SignatureProvider.keyOrSigToString(privateKeyBytes)
        val publicKeyHex = SignatureProvider.keyOrSigToString(publicKeyBytes)

        assertTrue(privateKeyHex.length % 2 == 0, "Private key hex should have even length")
        assertTrue(publicKeyHex.length % 2 == 0, "Public key hex should have even length")
        assertTrue(privateKeyHex.matches(Regex("[0-9a-f]+")), "Private key should be valid hex")
        assertTrue(publicKeyHex.matches(Regex("[0-9a-f]+")), "Public key should be valid hex")

        // Test conversion back to bytes
        val privateKeyBytesFromHex = SignatureProvider.keyOrSigToByteArray(privateKeyHex)
        val publicKeyBytesFromHex = SignatureProvider.keyOrSigToByteArray(publicKeyHex)

        assertTrue(
            privateKeyBytes.contentEquals(privateKeyBytesFromHex),
            "Private key should survive hex round-trip conversion"
        )
        assertTrue(
            publicKeyBytes.contentEquals(publicKeyBytesFromHex),
            "Public key should survive hex round-trip conversion"
        )
    }

    @Test
    @DisplayName("Test data string conversions")
    fun testDataConversions() {
        val testString = "Test data for conversion"

        val dataBytes = SignatureProvider.dataToByteArray(testString)
        assertNotNull(dataBytes)
        assertTrue(dataBytes.isNotEmpty(), "Data bytes should not be empty")

        val dataString = SignatureProvider.dataToString(dataBytes)
        assertEquals(testString, dataString, "String should survive round-trip conversion")

        val testDataBytes = SignatureProvider.dataToByteArray(testData)
        val testDataString = SignatureProvider.dataToString(testDataBytes)
        assertEquals(testData, testDataString, "Test data should survive round-trip conversion")
    }

    @Test
    @DisplayName("Test sign and verify with hex strings")
    fun testHexSignAndVerify() {
        val algorithm = "ECDSA"
        val keyPair = SignatureProvider.generateKeyPair(algorithm)

        val privateKeyHex = SignatureProvider.keyOrSigToString(keyPair.private.encoded)
        val publicKeyHex = SignatureProvider.keyOrSigToString(keyPair.public.encoded)

        val signature = SignatureProvider.sign(testData, privateKeyHex, algorithm)
        assertNotNull(signature)

        val signatureHex = SignatureProvider.keyOrSigToString(signature)

        val isValid = SignatureProvider.verify(testData, signatureHex, publicKeyHex, algorithm)
        assertTrue(isValid, "Signature should be valid when using hex string methods")
    }
}