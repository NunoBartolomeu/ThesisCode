package com.ledger.app.utils.hash.implementations

import com.ledger.app.utils.hash.HashAlgorithm
import com.ledger.app.utils.hash.RegisterOnHashProvider
import java.security.MessageDigest

@RegisterOnHashProvider
class SHA256Algorithm : HashAlgorithm {
    override val name = "SHA-256"
    override fun hash(data: String): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
    }
}