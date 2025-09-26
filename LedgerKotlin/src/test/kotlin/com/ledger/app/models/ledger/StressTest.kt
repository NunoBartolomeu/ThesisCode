package com.ledger.app.models.ledger

import com.ledger.app.utils.signature.SignatureProvider
import org.junit.jupiter.api.Test
import java.security.KeyPair
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.random.Random

class LedgerStressTest {

    data class TestUser(val name: String, val keyPair: KeyPair)

    private inline fun <T> calculateElapsedTime(block: () -> T): T {
        val start = System.nanoTime()
        val result = block()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        println("Elapsed time: ${elapsedMs} ms")
        return result
    }

    private inline fun <T> measureTime(block: () -> T): Pair<T, Long> {
        val start = System.nanoTime()
        val result = block()
        val elapsedNs = System.nanoTime() - start
        return result to elapsedNs
    }


    private fun generateUsers(num: Int, algo: String): List<TestUser> {
        val keyPair = SignatureProvider.generateKeyPair(algo)
        return (1..num).map { i ->
            TestUser("User$i", keyPair)
        }
    }

    private fun signEntry(entry: Entry, user: TestUser, algo: String): Entry.Signature {
        val signatureData = SignatureProvider.sign(entry.hash.toByteArray(), user.keyPair.private, algo)
        return Entry.Signature(
            signerId = user.name,
            signatureData = SignatureProvider.toHexString(signatureData),
            publicKey = SignatureProvider.toHexString(user.keyPair.public.encoded),
            algorithm = algo
        )
    }

    fun stressLedger(
        numEntries: Int = 1000,
        entriesPerPage: Int = 10,
        numSigPerEntry: Int = 3,
        contentSize: Int = 1024,
        hashAlgorithm: String = "SHA-256",
        signAlgorithm: String = "ECDSA",
        numThreads: Int = 12
    ) {
        val users = generateUsers(10, signAlgorithm)
        val ledger = Ledger(
            LedgerConfig("TestLedger", entriesPerPage, hashAlgorithm)
        )

        val executor = Executors.newFixedThreadPool(numThreads)
        val rand = Random(System.currentTimeMillis())

        val entryIds: MutableList<String?> = MutableList(numEntries) { null }
        val times = mutableListOf<Long>()

        repeat(numEntries) { idx ->
            executor.submit {
                val (entry, elapsed) = measureTime {
                    val chosenSenders = users.shuffled(rand).take(numSigPerEntry)

                    val content = "a".repeat(contentSize)
                    val entry = ledger.createEntry(
                        content = content,
                        senders = chosenSenders.map { it.name },
                        recipients = emptyList(),
                        relatedEntries = emptyList(),
                        keywords = emptyList()
                    )

                    synchronized(entryIds) {
                        entryIds[idx] = entry.id
                    }

                    // sign with chosen senders
                    chosenSenders.forEach { user ->
                        val sig = signEntry(entry, user, signAlgorithm)
                        ledger.addSignature(entry.id, sig)
                    }

                    entry
                }

                synchronized(times) {
                    times.add(elapsed)
                }
            }
        }

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.MINUTES)

        println("Unverified: ${ledger.unverifiedEntries.count()}")
        ledger.unverifiedEntries.forEach { entry ->
            println(entry)
        }
        println("Verified: ${ledger.verifiedEntries.count()}")
        ledger.verifiedEntries.forEach { entry ->
            println(entry)
        }
        println("Pages: ${ledger.pages.count()}")
        println()
        ledger.pages.forEach { page ->
            println(page)
        }
        println()
        ledger.pages.forEach { page ->
            val entryNumbers = page.entries.mapNotNull { entry ->
                entryIds.indexOf(entry.id).takeIf { it >= 0 }
            }
            println(
                "Page(ledger=${page.ledgerName}, " +
                        "number=${page.number}, " +
                        "timestamp=${page.timestamp}, " +
                        "merkleRoot=${page.merkleRoot.take(4)}, " +
                        "prevHash=${page.previousHash?.take(4)}, " +
                        "hash=${page.hash.take(4)}, " +
                        "entries=$entryNumbers)"
            )
        }

        val avgMs = (times.average() / 1_000_000).roundToInt()
        println("Average Entry time $avgMs ms")
        val highest = (times.max() / 1_000_000)
        println("Max Entry time $highest ms")
        val lowest = (times.min() / 1_000_000)
        println("Min Entry time $lowest ms")

        TODO("Try to figure out the top 5 longest and when they happened and the top 5 lowest and where they happen, also standard deviation, no outliers...")
    }

    @Test
    fun `default stress`() {
        calculateElapsedTime {
            stressLedger()
        }
    }
}
