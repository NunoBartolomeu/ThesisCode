package com.ledger.app.utils.encryption

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EncryptionProviderTest {

    private val testData = "Hello, World!"

    @Test
    fun `Test encrypt and decrypt for all supported algorithms`() {
        val algorithms = EncryptionProvider.getSupportedAlgorithms()
        assertTrue(algorithms.isNotEmpty(), "No encryption algorithms registered")

        algorithms.forEach { algorithm ->
            val payload = EncryptionProvider.encrypt(testData, algorithm)
            assertNotNull(payload, "Payload should not be null for $algorithm")
            assertTrue(payload.cipherText.isNotEmpty(), "Ciphertext should not be empty for $algorithm")

            val decryptedBytes = EncryptionProvider.decrypt(payload, payload.encryptedKey, algorithm)
            val decryptedText = String(decryptedBytes)

            assertEquals(testData, decryptedText, "Decrypted text should match original for $algorithm")
        }
    }
}
