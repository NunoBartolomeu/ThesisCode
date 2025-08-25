package com.ledger.app.utils.signature.implementations

import com.ledger.app.utils.signature.RegisterOnSignatureProvider
import com.ledger.app.utils.signature.SignatureAlgorithm
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

@RegisterOnSignatureProvider
class RSASignatureAlgorithm(private val keySize: Int = 2048) : SignatureAlgorithm {
    override val name: String = "RSA"

    companion object {
        private const val RSA_ALGORITHM = "RSA"
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
    }

    override fun sign(data: ByteArray, privateKey: PrivateKey): ByteArray {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    override fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean {
        val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
        sig.initVerify(publicKey)
        sig.update(data)
        return sig.verify(signature)
    }

    override fun generateKeyPair(): KeyPair {
        return KeyPairGenerator.getInstance(RSA_ALGORITHM).apply {
            initialize(keySize)
        }.generateKeyPair()
    }

    override fun bytesToPublicKey(encodedPublicKey: ByteArray): PublicKey {
        val spec = X509EncodedKeySpec(encodedPublicKey)
        return KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(spec)
    }

    override fun bytesToPrivateKey(encodedPrivateKey: ByteArray): PrivateKey {
        val spec = PKCS8EncodedKeySpec(encodedPrivateKey)
        return KeyFactory.getInstance(RSA_ALGORITHM).generatePrivate(spec)
    }
}