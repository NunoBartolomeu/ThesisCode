package com.ledger.app.models.ledger

import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider
import org.junit.jupiter.api.Test
import java.security.KeyPair
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class LedgerStressTest {

    data class TestUser(val name: String, val keyPair: KeyPair)

    data class EntryTiming(
        val entryId: String,
        val numSignatures: Int,
        val entryCreationTimeMs: Double,
        val signTimesMs: List<Double>,
    )

    data class TestResults(
        val config: TestConfig,
        val ledger: Ledger,
        val totalTimeMs: Double, // Excluding setup
        val avgEntryCreationTimeMs: Double,
        val avgSignatureTimeMs: Double,
        val avgPageCreationTimeMs: Double,
        val totalEntries: Int,
        val totalSignatures: Int,
        val totalPages: Int
    )

    private inline fun <T> measureTimeMs(block: () -> T): Pair<T, Double> {
        val start = System.nanoTime()
        val result = block()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        return result to elapsedMs
    }

    private fun generateUsers(num: Int, algo: String): List<TestUser> {
        val keyPair = SignatureProvider.generateKeyPair(algo)
        return (1..num).map { i -> TestUser("User$i", keyPair) }
    }

    data class TestConfig(
        val numEntries: Int,
        val entriesPerPage: Int,
        val numSigPerEntry: Int,
        val contentSize: Int,
        val hashAlgorithm: String,
        val signAlgorithm: String,
        val numThreads: Int
    )

    fun stressLedger(
        numEntries: Int = 10_000,
        entriesPerPage: Int = 100,
        numSigPerEntry: Int = 3,
        contentSize: Int = 2048,
        numThreads: Int = 12,
        hashAlgorithm: String = "SHA-256",
        signAlgorithm: String = "ECDSA",
    ): TestResults {
        //Activate providers to dodge reflection
        HashProvider.toHexString("clear".toByteArray())
        SignatureProvider.toHexString("clear".toByteArray())

        // Setup (not counted in test time)
        val config = TestConfig(numEntries, entriesPerPage, numSigPerEntry, contentSize, hashAlgorithm, signAlgorithm, numThreads)
        val users = generateUsers(10, signAlgorithm)
        val ledger = Ledger(LedgerConfig("TestLedger", entriesPerPage, hashAlgorithm))

        val executor = Executors.newFixedThreadPool(numThreads)
        val rand = Random(System.currentTimeMillis())
        val timings = mutableListOf<EntryTiming>()

        // Measure only the actual test execution time
        val (_, totalTimeMs) = measureTimeMs {
            repeat(numEntries) { _ ->
                executor.submit {
                    val chosenSenders = users.shuffled(rand).take(numSigPerEntry)
                    val content = "a".repeat(contentSize)

                    // Measure entry creation time
                    val (entry, entryCreationTime) = measureTimeMs {
                        ledger.createEntry(
                            content = content,
                            senders = chosenSenders.map { it.name },
                            recipients = emptyList(),
                            relatedEntries = emptyList(),
                            keywords = emptyList()
                        )
                    }

                    // Measure each signature addition time
                    val signatureTimes = mutableListOf<Double>()
                    chosenSenders.forEach { user ->
                        val (_, sigTime) = measureTimeMs {
                            val signatureData = SignatureProvider.sign(entry.hash.toByteArray(), user.keyPair.private, signAlgorithm)
                            val sig = Entry.Signature(
                                signerId = user.name,
                                signatureData = SignatureProvider.toHexString(signatureData),
                                publicKey = SignatureProvider.toHexString(user.keyPair.public.encoded),
                                algorithm = signAlgorithm
                            )
                            ledger.addSignature(entry.id, sig)
                        }
                        signatureTimes.add(sigTime)
                    }

                    synchronized(timings) {
                        timings.add(EntryTiming(
                            entryId = entry.id,
                            numSignatures = numSigPerEntry,
                            entryCreationTimeMs = entryCreationTime,
                            signTimesMs = signatureTimes,
                        ))
                    }
                }
            }

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.MINUTES)
    }

        // Calculate totals
        val totalEntries = numEntries
        val totalSignatures = numEntries * numSigPerEntry
        val totalPages = numEntries / entriesPerPage

        // Calculate averages
        val avgCreationTime = timings.map { it.entryCreationTimeMs }.average()
        val avgSignatureTime = timings.flatMap { it.signTimesMs }.average()
        val avgPageCreationTime = ledger.pageCreationTimesInMs.average()

        val results = TestResults(
            config = config,
            ledger = ledger,
            totalTimeMs = totalTimeMs,
            avgEntryCreationTimeMs = avgCreationTime,
            avgSignatureTimeMs = avgSignatureTime,
            avgPageCreationTimeMs = avgPageCreationTime,
            totalEntries = totalEntries,
            totalSignatures = totalSignatures,
            totalPages = totalPages
        )

        printResults(results)
        return results
    }

    private fun printResults(results: TestResults) {
        println("\n" + "=".repeat(60))
        println(" STRESS TEST RESULTS")
        println("=".repeat(60))

        // Config
        println("\nConfig: ")
        if (results.config.numEntries != 10_000)        println("   Number of Entries: ${results.config.numEntries}")
        if (results.config.numSigPerEntry != 3)         println("   Signatures per Entry: ${results.config.numSigPerEntry}")
        if (results.config.entriesPerPage != 100)       println("   Entries per Page: ${results.config.entriesPerPage}")
        if (results.config.contentSize != 2048)         println("   Content Size: ${results.config.contentSize} (Bytes)")
        if (results.config.numThreads != 12)            println("   Number of Threads: ${results.config.numThreads}")
        if (results.config.hashAlgorithm != "SHA-256")  println("   Hash Algorithm: ${results.config.hashAlgorithm}")
        if (results.config.signAlgorithm != "ECDSA")    println("   Signature Algorithm: ${results.config.signAlgorithm}")

        /*
        // Totals
        println("\nTotals:")
        println("   Total Entries: ${results.totalEntries}")
        println("   Total Signatures: ${results.totalSignatures}")
        println("   Total Pages: ${results.totalPages}")
        */
        // Timing Results
        println("\nTiming Results:")
        println("   Total Test Time: ${String.format("%.0f", results.totalTimeMs)} ms")
        println("   Average Entry Creation Time: ${String.format("%.4f", results.avgEntryCreationTimeMs)} ms")
        println("   Average Signature Creation Time: ${String.format("%.4f", results.avgSignatureTimeMs)} ms")
        println("   Average Page Creation Time: ${String.format("%.4f", results.avgPageCreationTimeMs)} ms")
        /*
        // Ledger State
        println("\nLedger State:")
        println("   Verified Entries: ${results.ledger.verifiedEntries.count()}")
        println("   Unverified Entries: ${results.ledger.unverifiedEntries.count()}")
        println("   Total Pages Created: ${results.ledger.pages.size}")
        println("   Expected State: ${results.ledger.verifiedEntries.count() == 0 && results.ledger.unverifiedEntries.count() == 0 && results.ledger.pages.count() == results.config.numEntries / results.config.entriesPerPage}")
        */
    }




    @Test
    fun `signature timing comparison test`() {
        println("\n" + "=".repeat(80))
        println(" SIGNATURE TIMING COMPARISON")
        println("=".repeat(80))

        stressLedger(signAlgorithm = "ECDSA")
        stressLedger(signAlgorithm = "ECDSA")
        stressLedger(signAlgorithm = "ECDSA")
        stressLedger(signAlgorithm = "ECDSA")
        stressLedger(signAlgorithm = "ECDSA")

        stressLedger(signAlgorithm = "RSA")
        stressLedger(signAlgorithm = "RSA")
        stressLedger(signAlgorithm = "RSA")
        stressLedger(signAlgorithm = "RSA")
        stressLedger(signAlgorithm = "RSA")
    }

    @Test
    fun `hash timing comparison test`() {
        println("\n" + "=".repeat(80))
        println(" HASH TIMING COMPARISON")
        println("=".repeat(80))

        stressLedger(hashAlgorithm = "SHA-256")
        stressLedger(hashAlgorithm = "SHA-256")
        stressLedger(hashAlgorithm = "SHA-256")
        stressLedger(hashAlgorithm = "SHA-256")
        stressLedger(hashAlgorithm = "SHA-256")

        stressLedger(hashAlgorithm = "SHA-512")
        stressLedger(hashAlgorithm = "SHA-512")
        stressLedger(hashAlgorithm = "SHA-512")
        stressLedger(hashAlgorithm = "SHA-512")
        stressLedger(hashAlgorithm = "SHA-512")

        stressLedger(hashAlgorithm = "SHA3-256")
        stressLedger(hashAlgorithm = "SHA3-256")
        stressLedger(hashAlgorithm = "SHA3-256")
        stressLedger(hashAlgorithm = "SHA3-256")
        stressLedger(hashAlgorithm = "SHA3-256")
    }

    @Test
    fun `Threads timing comparison test`() {
        println("\n" + "=".repeat(80))
        println(" THREADS TIMING COMPARISON")
        println("=".repeat(80))

        stressLedger(numThreads = 1)
        stressLedger(numThreads = 1)
        stressLedger(numThreads = 1)
        stressLedger(numThreads = 1)
        stressLedger(numThreads = 1)

        stressLedger(numThreads = 2)
        stressLedger(numThreads = 2)
        stressLedger(numThreads = 2)
        stressLedger(numThreads = 2)
        stressLedger(numThreads = 2)

        stressLedger(numThreads = 4)
        stressLedger(numThreads = 4)
        stressLedger(numThreads = 4)
        stressLedger(numThreads = 4)
        stressLedger(numThreads = 4)

        stressLedger(numThreads = 8)
        stressLedger(numThreads = 8)
        stressLedger(numThreads = 8)
        stressLedger(numThreads = 8)
        stressLedger(numThreads = 8)

        stressLedger(numThreads = 12)
        stressLedger(numThreads = 12)
        stressLedger(numThreads = 12)
        stressLedger(numThreads = 12)
        stressLedger(numThreads = 12)

        stressLedger(numThreads = 16)
        stressLedger(numThreads = 16)
        stressLedger(numThreads = 16)
        stressLedger(numThreads = 16)
        stressLedger(numThreads = 16)
    }

    @Test
    fun `Content size timing comparison test`() {
        println("\n" + "=".repeat(80))
        println(" CONTENT SIZE TIMING COMPARISON")
        println("=".repeat(80))

        stressLedger(contentSize = 516)
        stressLedger(contentSize = 516)
        stressLedger(contentSize = 516)
        stressLedger(contentSize = 516)
        stressLedger(contentSize = 516)

        stressLedger(contentSize = 1024)
        stressLedger(contentSize = 1024)
        stressLedger(contentSize = 1024)
        stressLedger(contentSize = 1024)
        stressLedger(contentSize = 1024)

        stressLedger(contentSize = 2048)
        stressLedger(contentSize = 2048)
        stressLedger(contentSize = 2048)
        stressLedger(contentSize = 2048)
        stressLedger(contentSize = 2048)

        stressLedger(contentSize = 4096)
        stressLedger(contentSize = 4096)
        stressLedger(contentSize = 4096)
        stressLedger(contentSize = 4096)
        stressLedger(contentSize = 4096)

        stressLedger(contentSize = 8192)
        stressLedger(contentSize = 8192)
        stressLedger(contentSize = 8192)
        stressLedger(contentSize = 8192)
        stressLedger(contentSize = 8192)

        stressLedger(contentSize = 16384)
        stressLedger(contentSize = 16384)
        stressLedger(contentSize = 16384)
        stressLedger(contentSize = 16384)
        stressLedger(contentSize = 16384)
    }

    @Test
    fun `Entries per page size timing comparison test`() {
        println("\n" + "=".repeat(80))
        println(" ENTRIES PER PAGE TIMING COMPARISON")
        println("=".repeat(80))

        stressLedger(entriesPerPage = 10)
        stressLedger(entriesPerPage = 10)
        stressLedger(entriesPerPage = 10)
        stressLedger(entriesPerPage = 10)
        stressLedger(entriesPerPage = 10)

        stressLedger(entriesPerPage = 50)
        stressLedger(entriesPerPage = 50)
        stressLedger(entriesPerPage = 50)
        stressLedger(entriesPerPage = 50)
        stressLedger(entriesPerPage = 50)

        stressLedger(entriesPerPage = 100)
        stressLedger(entriesPerPage = 100)
        stressLedger(entriesPerPage = 100)
        stressLedger(entriesPerPage = 100)
        stressLedger(entriesPerPage = 100)

        stressLedger(entriesPerPage = 500)
        stressLedger(entriesPerPage = 500)
        stressLedger(entriesPerPage = 500)
        stressLedger(entriesPerPage = 500)
        stressLedger(entriesPerPage = 500)

        stressLedger(entriesPerPage = 1000)
        stressLedger(entriesPerPage = 1000)
        stressLedger(entriesPerPage = 1000)
        stressLedger(entriesPerPage = 1000)
        stressLedger(entriesPerPage = 1000)
    }

    @Test
    fun `Signature per entry timing comparison test`() {
        println("\n" + "=".repeat(80))
        println(" SIGNATURES PER ENTRY TIMING COMPARISON")
        println("=".repeat(80))

        stressLedger(numSigPerEntry = 1)
        stressLedger(numSigPerEntry = 1)
        stressLedger(numSigPerEntry = 1)
        stressLedger(numSigPerEntry = 1)
        stressLedger(numSigPerEntry = 1)

        stressLedger(numSigPerEntry = 3)
        stressLedger(numSigPerEntry = 3)
        stressLedger(numSigPerEntry = 3)
        stressLedger(numSigPerEntry = 3)
        stressLedger(numSigPerEntry = 3)

        stressLedger(numSigPerEntry = 5)
        stressLedger(numSigPerEntry = 5)
        stressLedger(numSigPerEntry = 5)
        stressLedger(numSigPerEntry = 5)
        stressLedger(numSigPerEntry = 5)

        stressLedger(numSigPerEntry = 10)
        stressLedger(numSigPerEntry = 10)
        stressLedger(numSigPerEntry = 10)
        stressLedger(numSigPerEntry = 10)
        stressLedger(numSigPerEntry = 10)
    }

    @Test
    fun `Number of entries timing comparison test`() {
        println("\n" + "=".repeat(80))
        println(" NUMBER OF ENTRIES TIMING COMPARISON")
        println("=".repeat(80))

        stressLedger(numEntries = 1000)
        stressLedger(numEntries = 1000)
        stressLedger(numEntries = 1000)
        stressLedger(numEntries = 1000)
        stressLedger(numEntries = 1000)

        stressLedger(numEntries = 5000)
        stressLedger(numEntries = 5000)
        stressLedger(numEntries = 5000)
        stressLedger(numEntries = 5000)
        stressLedger(numEntries = 5000)

        stressLedger(numEntries = 10000)
        stressLedger(numEntries = 10000)
        stressLedger(numEntries = 10000)
        stressLedger(numEntries = 10000)
        stressLedger(numEntries = 10000)

        stressLedger(numEntries = 50000)
        stressLedger(numEntries = 50000)
        stressLedger(numEntries = 50000)
        stressLedger(numEntries = 50000)
        stressLedger(numEntries = 50000)

        stressLedger(numEntries = 100000)
        stressLedger(numEntries = 100000)
        stressLedger(numEntries = 100000)
        stressLedger(numEntries = 100000)
        stressLedger(numEntries = 100000)
    }
}