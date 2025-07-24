package com.ledger.app.utils.implementations

import com.ledger.app.utils.CryptoProvider
import com.ledger.app.utils.EncryptedPayload
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class RSACryptoProvider(private val keySize: Int = 2048) : CryptoProvider {
    override val algorithm: String = RSA_ALGORITHM

    companion object {
        private const val RSA_ALGORITHM = "RSA"
        private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
        private const val AES_ALGORITHM = "AES"
        private const val AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val AES_KEY_SIZE = 256 // bits
        private const val AES_IV_SIZE = 16   // bytes
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
    }

    override fun encrypt(data: String, publicKey: PublicKey): EncryptedPayload {
        val aesKey = KeyGenerator.getInstance(AES_ALGORITHM).apply { init(AES_KEY_SIZE) }.generateKey()
        val iv = ByteArray(AES_IV_SIZE).also { SecureRandom().nextBytes(it) }

        val aesCipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, aesKey, IvParameterSpec(iv))
        }
        val cipherText = aesCipher.doFinal(dataToByteArray(data))

        val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, publicKey)
        }
        val encryptedKey = rsaCipher.doFinal(aesKey.encoded)

        return EncryptedPayload(encryptedKey, iv, cipherText)
    }

    override fun decrypt(encryptedPayload: EncryptedPayload, privateKey: PrivateKey): ByteArray {
        val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, privateKey)
        }
        val aesKeyBytes = rsaCipher.doFinal(encryptedPayload.encryptedKey)
        val aesKey = SecretKeySpec(aesKeyBytes, AES_ALGORITHM)

        val aesCipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(encryptedPayload.iv))
        }
        return aesCipher.doFinal(encryptedPayload.cipherText)
    }

    override fun sign(data: String, privateKey: PrivateKey): ByteArray {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(dataToByteArray(data))
        return signature.sign()
    }

    override fun verify(data: String, signatureBytes: ByteArray, publicKey: PublicKey): Boolean {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initVerify(publicKey)
        signature.update(dataToByteArray(data))
        return signature.verify(signatureBytes)
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
