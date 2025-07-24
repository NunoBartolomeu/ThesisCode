package com.ledger.app.services.sys_kp.implementations

import com.ledger.app.services.sys_kp.SysKeyPairService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.CryptoProvider
import com.ledger.app.utils.HashProvider
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyPair
import java.security.Security
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.time.LocalDateTime
import java.util.Date

@Service
class SysKeyPairServiceLocal(
    @Value("\${system.key.path:./config/system_private_key.pem}") private val keyPathStr: String,
    private val cryptoProvider: CryptoProvider,
): SysKeyPairService {
    private val logger = ColorLogger("SystemKeyPair", RGB.ORANGE_BRIGHT, LogLevel.DEBUG)
    private var keyPair: KeyPair
    private var certificate: X509Certificate
    override fun getKeyPair(): KeyPair = keyPair
    private val keyPath = Paths.get(keyPathStr)
    private val certPath = keyPath.parent.resolve("system_certificate.crt")
    private val oldDir = keyPath.parent.resolve("old")

    init {
        Security.addProvider(BouncyCastleProvider())

        val (k, c) = initializeKeyPairAndCert()
        keyPair = k
        certificate = c
        logger.debug("Key Pair and Certificate are set")
    }

    private fun initializeKeyPairAndCert(): Pair<KeyPair, X509Certificate> {
        if (Files.exists(keyPath) && Files.exists(certPath)) {
            try {
                val cert = loadCertificate()
                val key = loadPrivateKey()
                if (testKeyAndCertificate(cert, key)) {
                    logger.debug("Key and certificate are valid")
                    return key to cert
                }
            } catch (e: Exception) {
                logger.warn("Failed to load key/cert: ${e.message}")
            }
        } else {
            logger.warn("Key or certificate file missing")
        }
        moveToOld()
        val newKeyPair = createKeyPair()
        savePrivateKey(newKeyPair)
        val newCert = createCertificate(newKeyPair)
        saveCertificate(newCert)
        if (testKeyAndCertificate(newCert, newKeyPair)) {
            logger.info("New system key pair and certificate created")
            return newKeyPair to newCert
        } else {
            throw Exception("There is a problem generating valid keys or certificates")
        }
    }

    fun testKeyAndCertificate(certificate: X509Certificate, keyPair: KeyPair): Boolean {
        try {
            certificate.checkValidity()
        } catch (e: CertificateExpiredException) {
            logger.warn("Certificate is expired")
            return false
        } catch (e: CertificateNotYetValidException) {
            logger.warn("Certificate is not yet valid")
            return false
        }

        try {
            certificate.verify(certificate.publicKey)
        } catch (e: Exception) {
            logger.warn("Certificate is not self signed")
            return false
        }

        val test = "test"
        val signature = cryptoProvider.sign(test, keyPair.private)
        if (!cryptoProvider.verify(test, signature, certificate.publicKey)) {
            logger.warn("Certificate and key do not match")
            return false
        }

        return true
    }

    private fun createKeyPair(): KeyPair = cryptoProvider.generateKeyPair()

     private fun savePrivateKey(keyPair: KeyPair) {
         Files.createDirectories(keyPath.parent)
         PemWriter(Files.newBufferedWriter(keyPath)).use {
             it.writeObject(PemObject("PRIVATE KEY", keyPair.private.encoded))
         }
     }

     private fun createCertificate(keyPair: KeyPair): X509Certificate {
         val now = Date()
         val end = Date(now.time + 365L * 24 * 60 * 60 * 1000) // 1 year
         val dn = X500Name("CN=System")

         val certBuilder = JcaX509v3CertificateBuilder(
             dn, BigInteger.ONE, now, end, dn, keyPair.public
         )
         val signer = JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC")
             .build(keyPair.private)
         val certHolder = certBuilder.build(signer)
         return JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder)
     }

     private fun saveCertificate(cert: X509Certificate) {
         PemWriter(Files.newBufferedWriter(certPath)).use {
             it.writeObject(PemObject("CERTIFICATE", cert.encoded))
         }
     }

     private fun loadPrivateKey(): KeyPair {
         Files.newBufferedReader(keyPath).use { reader ->
             PEMParser(reader).use { parser ->
                 val converter = JcaPEMKeyConverter().setProvider("BC")
                 val obj = parser.readObject()
                 return when (obj) {
                     is PEMKeyPair -> converter.getKeyPair(obj)
                     is PrivateKeyInfo -> {
                         val privateKey = converter.getPrivateKey(obj)
                         val publicKey = loadCertificate().publicKey
                         KeyPair(publicKey, privateKey)
                     }

                     else -> throw IllegalArgumentException("Unsupported key format")
                 }
             }
         }
     }

     private fun loadCertificate(): X509Certificate {
         Files.newBufferedReader(certPath).use { reader ->
             PEMParser(reader).use { parser ->
                 val holder = parser.readObject() as X509CertificateHolder
                 return JcaX509CertificateConverter().setProvider("BC").getCertificate(holder)
             }
         }
     }

    private fun moveToOld() {
        Files.createDirectories(oldDir)
        val timestamp = LocalDateTime.now().toString().replace(":", "-")
        if (Files.exists(keyPath)) {
            val target = oldDir.resolve("system_private_key_$timestamp.pem")
            Files.move(keyPath, target)
        }
        if (Files.exists(certPath)) {
            val target = oldDir.resolve("system_certificate_$timestamp.crt")
            Files.move(certPath, target)
        }
    }
}