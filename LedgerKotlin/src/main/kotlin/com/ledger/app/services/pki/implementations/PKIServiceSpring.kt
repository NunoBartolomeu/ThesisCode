package com.ledger.app.services.pki.implementations

import com.ledger.app.repositories.pki.PKIRepo
import com.ledger.app.services.pki.PublicKeyInfrastructureService
import com.ledger.app.utils.*
import com.ledger.app.utils.signature.SignatureProvider
import jakarta.annotation.PostConstruct
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.KeyPair
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.util.Date

@Service
class PKIServiceSpring(
    private val pkiRepo: PKIRepo
): PublicKeyInfrastructureService {
    @Value("\${app.logLevel:INFO}")
    private lateinit var logLevelStr: String
    private lateinit var logger: ColorLogger
    private lateinit var systemKeyPair: KeyPair
    private lateinit var systemCertificate: Certificate

    companion object {
        private const val ONE_YEAR_MILLIS = 365L * 24 * 60 * 60 * 1000
        private const val SIGNATURE_ALGORITHM = "SHA256WithRSA"
        private const val PROVIDER = "BC"
        private const val SYSTEM_DN = "CN=LedgerSystem"
        private const val SYSTEM_SUBJECT_DN = "CN=System"
        private const val USER_DN_PREFIX = "CN=User-"
        private const val TEST_MESSAGE = "test"
    }

    override fun getSystemKeyPair(): KeyPair = systemKeyPair

    override fun getSystemCertificate(): Certificate = systemCertificate

    override fun getUserCertificate(userId: String): Certificate? = pkiRepo.loadUserCertificate(userId)

    override fun associatePublicKeyToUser(userId: String, hexPublicKey: String, algorithm: String): Certificate {
        val publicKeyBytes = SignatureProvider.keyOrSigToByteArray(hexPublicKey)
        val publicKey = SignatureProvider.resolve(algorithm).bytesToPublicKey(publicKeyBytes)

        val userCertificate = createUserCertificate(userId, publicKey)
        pkiRepo.saveUserCertificate(userId, userCertificate)
        return userCertificate
    }

    override fun verifyCertificate(userId: String, certificate: Certificate): Boolean {
        if (certificate !is X509Certificate) return false

        try {
            certificate.checkValidity()
        } catch (e: Exception) {
            logger.warn("Certificate for user $userId is not valid: ${e.message}")
            return false
        }

        return try {
            certificate.verify(systemCertificate.publicKey)
            true
        } catch (e: Exception) {
            logger.warn("Certificate verification failed for user $userId: ${e.message}")
            false
        }
    }

    @PostConstruct
    fun initialize() {
        logger = ColorLogger("PKI Service", RGB.ORANGE, logLevelStr)
        val (keyPair, certificate) = initializeSystemKeyAndCertificate()
        systemKeyPair = keyPair
        systemCertificate = certificate
        logger.debug("System Key Pair and Certificate initialized")
    }

    private fun initializeSystemKeyAndCertificate(): Pair<KeyPair, Certificate> {
        if (pkiRepo.systemFilesExist()) {
            val certificate = pkiRepo.loadSystemCertificate()
            val keyPair = pkiRepo.loadSystemKeyPair()

            if (certificate != null && keyPair != null) {
                return if (isValidKeyAndCertificate(certificate as X509Certificate, keyPair)) {
                    logger.debug("Loaded valid system key and certificate")
                    keyPair to certificate
                } else {
                    logger.warn("Existing system key/certificate invalid, generating new ones")
                    generateAndSaveNewSystemCredentials()
                }
            }
        }

        logger.info("System files missing, generating new system key pair and certificate")
        return generateAndSaveNewSystemCredentials()
    }

    private fun generateAndSaveNewSystemCredentials(): Pair<KeyPair, X509Certificate> {
        val newKeyPair = SignatureProvider.generateKeyPair(null)
        val newCertificate = createSystemCertificate(newKeyPair)

        pkiRepo.saveSystemKeyAndCertificate(newKeyPair, newCertificate)

        return if (isValidKeyAndCertificate(newCertificate, newKeyPair)) {
            logger.info("Successfully generated new system credentials")
            newKeyPair to newCertificate
        } else {
            throw IllegalStateException("Failed to generate valid system credentials")
        }
    }

    private fun createUserCertificate(userId: String, publicKey: PublicKey): X509Certificate {
        val now = Date()
        val expiry = Date(now.time + ONE_YEAR_MILLIS)
        val userDN = X500Name("$USER_DN_PREFIX$userId")
        val issuerDN = X500Name(SYSTEM_DN)
        val serialNumber = BigInteger.valueOf(System.currentTimeMillis())

        val certBuilder = JcaX509v3CertificateBuilder(
            issuerDN, serialNumber, now, expiry, userDN, publicKey
        )

        val signer = JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .setProvider(PROVIDER)
            .build(systemKeyPair.private)

        val certHolder = certBuilder.build(signer)
        return JcaX509CertificateConverter()
            .setProvider(PROVIDER)
            .getCertificate(certHolder)
    }

    private fun createSystemCertificate(keyPair: KeyPair): X509Certificate {
        val now = Date()
        val expiry = Date(now.time + ONE_YEAR_MILLIS)
        val dn = X500Name(SYSTEM_SUBJECT_DN)

        val certBuilder = JcaX509v3CertificateBuilder(
            dn, BigInteger.ONE, now, expiry, dn, keyPair.public
        )

        val signer = JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .setProvider(PROVIDER)
            .build(keyPair.private)

        val certHolder = certBuilder.build(signer)
        return JcaX509CertificateConverter()
            .setProvider(PROVIDER)
            .getCertificate(certHolder)
    }

    private fun isValidKeyAndCertificate(certificate: X509Certificate, keyPair: KeyPair): Boolean {
        return isCertificateValid(certificate) &&
                isSelfSigned(certificate) &&
                keyMatchesCertificate(keyPair, certificate)
    }

    private fun isCertificateValid(certificate: X509Certificate): Boolean {
        return try {
            certificate.checkValidity()
            true
        } catch (e: CertificateExpiredException) {
            logger.warn("Certificate expired")
            false
        } catch (e: CertificateNotYetValidException) {
            logger.warn("Certificate not yet valid")
            false
        }
    }

    private fun isSelfSigned(certificate: X509Certificate): Boolean {
        return try {
            certificate.verify(certificate.publicKey)
            true
        } catch (e: Exception) {
            logger.warn("Certificate is not self-signed")
            false
        }
    }

    private fun keyMatchesCertificate(keyPair: KeyPair, certificate: X509Certificate): Boolean {
        return try {
            val signature = SignatureProvider.sign(TEST_MESSAGE, keyPair.private, null)
            val isValid = SignatureProvider.verify(TEST_MESSAGE, signature, certificate.publicKey, null)
            if (!isValid) {
                logger.warn("Private key does not match certificate public key")
            }
            isValid
        } catch (e: Exception) {
            logger.warn("Error verifying key/certificate match: ${e.message}")
            false
        }
    }
}