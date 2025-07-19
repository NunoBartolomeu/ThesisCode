package com.ledger.app.utils.implementations

import com.ledger.app.utils.CryptoProvider
import com.ledger.app.utils.EncryptedPayload
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ECCryptoProvider : CryptoProvider {
    override val algorithm: String = EC_ALGORITHM

    companion object {
        private const val EC_ALGORITHM = "EC"
        private const val EC_CURVE = "secp256r1"
        private const val ECDH_ALGORITHM = "ECDH"
        private const val ECDSA_ALGORITHM = "SHA256withECDSA"
        private const val HASH_ALGORITHM = "SHA-256"

        private const val AES_ALGORITHM = "AES"
        private const val AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val AES_KEY_SIZE = 256
        private const val AES_IV_SIZE = 16
    }

    override fun encrypt(data: ByteArray, publicKey: PublicKey): EncryptedPayload {
        val keyPairGen = KeyPairGenerator.getInstance(EC_ALGORITHM).apply {
            initialize(ECGenParameterSpec(EC_CURVE))
        }
        val ephemeralKeyPair = keyPairGen.generateKeyPair()

        val sharedSecret = generateSharedSecret(ephemeralKeyPair.private, publicKey)
        val aesKey = deriveAESKey(sharedSecret)

        val iv = ByteArray(AES_IV_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, aesKey, IvParameterSpec(iv))
        }
        val cipherText = cipher.doFinal(data)

        return EncryptedPayload(ephemeralKeyPair.public.encoded, iv, cipherText)
    }

    override fun decrypt(encryptedPayload: EncryptedPayload, privateKey: PrivateKey): ByteArray {
        val ephemeralPubKey = bytesToPublicKey(encryptedPayload.encryptedKey)
        val sharedSecret = generateSharedSecret(privateKey, ephemeralPubKey)
        val aesKey = deriveAESKey(sharedSecret)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(encryptedPayload.iv))
        }
        return cipher.doFinal(encryptedPayload.cipherText)
    }

    override fun sign(data: ByteArray, privateKey: PrivateKey): ByteArray {
        return Signature.getInstance(ECDSA_ALGORITHM).run {
            initSign(privateKey)
            update(data)
            sign()
        }
    }

    override fun verify(data: ByteArray, signatureBytes: ByteArray, publicKey: PublicKey): Boolean {
        return Signature.getInstance(ECDSA_ALGORITHM).run {
            initVerify(publicKey)
            update(data)
            verify(signatureBytes)
        }
    }

    override fun generateKeyPair(): KeyPair {
        return KeyPairGenerator.getInstance(EC_ALGORITHM).apply {
            initialize(ECGenParameterSpec(EC_CURVE))
        }.generateKeyPair()
    }

    override fun bytesToPublicKey(data: ByteArray): PublicKey {
        val spec = X509EncodedKeySpec(data)
        return KeyFactory.getInstance(EC_ALGORITHM).generatePublic(spec)
    }

    override fun bytesToPrivateKey(data: ByteArray): PrivateKey {
        val spec = PKCS8EncodedKeySpec(data)
        return KeyFactory.getInstance(EC_ALGORITHM).generatePrivate(spec)
    }

    private fun generateSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        return KeyAgreement.getInstance(ECDH_ALGORITHM).run {
            init(privateKey)
            doPhase(publicKey, true)
            generateSecret()
        }
    }

    private fun deriveAESKey(sharedSecret: ByteArray): SecretKey {
        val hash = MessageDigest.getInstance(HASH_ALGORITHM).digest(sharedSecret)
        return SecretKeySpec(hash, 0, AES_KEY_SIZE / 8, AES_ALGORITHM)
    }
}
