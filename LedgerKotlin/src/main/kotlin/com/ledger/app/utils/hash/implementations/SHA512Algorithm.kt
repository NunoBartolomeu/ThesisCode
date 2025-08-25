package com.ledger.app.utils.hash.implementations

import com.ledger.app.utils.hash.HashAlgorithm
import com.ledger.app.utils.hash.RegisterOnHashProvider
import java.security.MessageDigest

@RegisterOnHashProvider
class SHA512Algorithm : HashAlgorithm {
    override val name = "SHA-512"
    override fun hash(data: String): ByteArray {
        return MessageDigest.getInstance("SHA-512").digest(data.toByteArray())
    }
}