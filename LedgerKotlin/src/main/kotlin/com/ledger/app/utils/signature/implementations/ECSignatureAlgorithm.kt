package com.ledger.app.utils.signature.implementations

import com.ledger.app.utils.signature.RegisterOnSignatureProvider
import com.ledger.app.utils.signature.SignatureAlgorithm
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

@RegisterOnSignatureProvider
class ECSignatureAlgorithm : SignatureAlgorithm {
    override val name: String = "ECDSA"

    companion object {
        private const val EC_ALGORITHM = "EC"
        private const val EC_CURVE = "secp256r1"
        private const val ECDSA_ALGORITHM = "SHA256withECDSA"
    }

    override fun sign(data: ByteArray, privateKey: PrivateKey): ByteArray {
        return Signature.getInstance(ECDSA_ALGORITHM).run {
            initSign(privateKey)
            update(data)
            sign()
        }
    }

    override fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean {
        return Signature.getInstance(ECDSA_ALGORITHM).run {
            initVerify(publicKey)
            update(data)
            verify(signature)
        }
    }

    override fun generateKeyPair(): KeyPair {
        return KeyPairGenerator.getInstance(EC_ALGORITHM).apply {
            initialize(ECGenParameterSpec(EC_CURVE))
        }.generateKeyPair()
    }

    override fun bytesToPublicKey(encodedPublicKey: ByteArray): PublicKey {
        val spec = X509EncodedKeySpec(encodedPublicKey)
        return KeyFactory.getInstance(EC_ALGORITHM).generatePublic(spec)
    }

    override fun bytesToPrivateKey(encodedPrivateKey: ByteArray): PrivateKey {
        val spec = PKCS8EncodedKeySpec(encodedPrivateKey)
        return KeyFactory.getInstance(EC_ALGORITHM).generatePrivate(spec)
    }
}