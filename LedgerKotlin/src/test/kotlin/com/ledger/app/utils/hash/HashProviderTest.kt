package com.ledger.app.utils.hash

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class HashProviderTest {

    private val testData = "Hello, World!"
    private val expectedAlgorithms = setOf("SHA-256", "SHA-512", "SHA3-256")

    @Test
    fun `has required algorithms registered`() {
        val supportedAlgorithms = HashProvider.getSupportedAlgorithms()

        assertEquals(supportedAlgorithms.count(), expectedAlgorithms.count(), "Missing / Extra algorithms found")

        supportedAlgorithms.forEach {
            println("$it is a supported algorithm")
        }

        expectedAlgorithms.forEach { algorithm ->
            assertTrue(
                supportedAlgorithms.contains(algorithm),
                "Algorithm '$algorithm' should be registered"
            )
        }
    }

    @Test
    fun `test SHA-256 algorithm directly`() {
        val algorithm = "SHA-256"
        val expected = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"

        // Test hashing
        val hashBytes = HashProvider.hash(testData, algorithm)
        assertNotNull(hashBytes)
        assertEquals(32, hashBytes.size, "SHA2-256 should produce 32 bytes")

        // Test toHashString conversion
        val hashString = HashProvider.toHexString(hashBytes)
        assertEquals(64, hashString.length, "SHA2-256 hex string should be 64 characters")
        assertTrue(hashString.matches(Regex("[0-9a-f]+")), "Hash should be valid hex")

        // Test against expected value
        assertEquals(expected, hashString, "SHA-256 hash should match expected value")
    }

    @Test
    fun `test SHA-512 algorithm directly`() {
        val algorithm = "SHA-512"
        val expected = "374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387"

        // Test hashing
        val hashBytes = HashProvider.hash(testData, algorithm)
        assertNotNull(hashBytes)
        assertEquals(64, hashBytes.size, "SHA2-512 should produce 64 bytes")

        // Test toHashString conversion
        val hashString = HashProvider.toHexString(hashBytes)
        assertEquals(128, hashString.length, "SHA2-512 hex string should be 128 characters")
        assertTrue(hashString.matches(Regex("[0-9a-f]+")), "Hash should be valid hex")

        // Test against expected value
        assertEquals(expected, hashString, "SHA-512 hash should match expected value")
    }

    @Test
    fun `test SHA3-256 algorithm directly`() {
        val algorithm = "SHA3-256"
        val expected = "1af17a664e3fa8e419b8ba05c2a173169df76162a5a286e0c405b460d478f7ef"

        // Test hashing
        val hashBytes = HashProvider.hash(testData, algorithm)
        assertNotNull(hashBytes)
        assertEquals(32, hashBytes.size, "SHA3-256 should produce 32 bytes")

        // Test toHashString conversion
        val hashString = HashProvider.toHexString(hashBytes)
        assertEquals(64, hashString.length, "SHA3-256 hex string should be 64 characters")
        assertTrue(hashString.matches(Regex("[0-9a-f]+")), "Hash should be valid hex")

        // Test against expected value
        assertEquals(expected, hashString, "SHA3-256 hash should match expected value")
    }
}