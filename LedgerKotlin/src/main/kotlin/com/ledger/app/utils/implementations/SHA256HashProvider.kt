package com.ledger.app.utils.implementations

import com.ledger.app.utils.HashProvider
import java.security.MessageDigest

class SHA256HashProvider: HashProvider {
    override val algorithm: String = "SHA-256"

    override fun hash(data: String): ByteArray {
        return MessageDigest.getInstance(algorithm).digest(toHashByteArray(data))
    }
}
