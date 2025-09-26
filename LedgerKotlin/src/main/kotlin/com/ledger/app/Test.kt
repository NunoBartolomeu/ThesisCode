import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Ledger<E>(
    private val threshold: Int = 3,          // number of signs before moving from unverifiedEntries → verifiedEntries
    private val entriesPerPage: Int = 10     // number of elements per page
) {

    private val unverifiedEntries = ConcurrentHashMap<String, Pair<Int, E>>() // A
    private val verifiedEntries = ConcurrentHashMap<String, E>()             // B
    val pages = ConcurrentLinkedQueue<List<E>>()                              // C

    private val batchLock = ReentrantLock()

    /** Add a new entry to unverifiedEntries */
    fun add(key: String, value: E) {
        unverifiedEntries.putIfAbsent(key, 0 to value)
    }

    /** Sign an existing entry in unverifiedEntries */
    fun sign(key: String) {
        unverifiedEntries.computeIfPresent(key) { _, entry ->
            val (count, value) = entry
            val newCount = count + 1
            if (newCount >= threshold) {
                // move to verifiedEntries
                verifiedEntries[key] = value
                tryCreatePageIfFull()
                null // remove from unverifiedEntries
            } else {
                newCount to value
            }
        }
    }

    /** Remove an element manually from verifiedEntries */
    fun removeVerified(key: String) {
        verifiedEntries.remove(key)
    }

    /** Read an element from any stage */
    fun read(key: String): E? {
        return unverifiedEntries[key]?.second ?: verifiedEntries[key]
    }

    /** Attempt to create a page if verifiedEntries is full */
    private fun tryCreatePageIfFull() {
        if (verifiedEntries.size >= entriesPerPage) {
            batchLock.withLock {
                if (verifiedEntries.size >= entriesPerPage) {
                    val snapshot = mutableListOf<E>()
                    val keysToRemove = mutableListOf<String>()

                    verifiedEntries.forEach { (key, value) ->
                        if (snapshot.size < entriesPerPage) {
                            snapshot.add(value)
                            keysToRemove.add(key)
                        }
                    }

                    keysToRemove.forEach { verifiedEntries.remove(it) }
                    pages.add(snapshot.toList())
                }
            }
        }
    }

    fun status() = Triple(unverifiedEntries.size, verifiedEntries.size, pages.size)
}

fun main() {
    val ledger = Ledger<Int>(threshold = 3, entriesPerPage = 10)
    val executor = Executors.newFixedThreadPool(8)

    repeat(10000) { i ->
        val key = "entry-$i"
        executor.submit {
            ledger.add(key, i)               // Add entry
            repeat(3) { ledger.sign(key) }   // Sign entry 3 times to reach threshold
        }
    }

    executor.shutdown()
    executor.awaitTermination(5, TimeUnit.MINUTES)

    // Print results
    val (unverified, verified, pages) = ledger.status()
    println("Unverified entries: $unverified")
    println("Verified entries: $verified")
    println("Pages created: $pages")

    println("\nPages content:")
    ledger.pages.forEachIndexed { index, page ->
        println("Page ${index + 1}: $page")
    }
}
