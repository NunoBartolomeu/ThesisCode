package com.ledger.app.services.sys_kp.implementations

import com.ledger.app.services.ledger.LedgerService
import com.ledger.app.services.sys_kp.SysKeyPairService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.CryptoProvider
import com.ledger.app.utils.HashProvider
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.Rgb
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
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.cert.X509Certificate
import java.time.LocalDateTime
import java.util.Date
import kotlin.math.log

@Service
class SysKeyPairServiceLocal(
    @Value("\${system.key.path:./config/system_private_key.pem}") private val keyPathStr: String,
    private val hashProvider: HashProvider,
    private val cryptoProvider: CryptoProvider,
): SysKeyPairService {
    private val KP_SYSTEM = "sys_key_pair_service"
    private val KP_LEDGER = "sys_key_pair_ledger"
    private val logger = ColorLogger("SystemKeyPair", Rgb(150, 150, 50), LogLevel.DEBUG)
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
        val keyExists = Files.exists(keyPath)
        val certExists = Files.exists(certPath)

        val status =
            if (keyExists && certExists) {
                try {
                    val cert = loadCertificate()
                    val key = loadPrivateKey()
                    if (validateCertificate(cert, key)) {
                        logger.info("Key loaded successfully")
                        return key to cert
                    } else {
                        logger.warn("Failed validation")
                        "failed validation"
                    }
                } catch (e: Exception) {
                    logger.warn("Failed loading")
                    "failed load"
                }
            } else {
                logger.warn("Files are missing")
                "missing files"
            }

        moveToOld()
        val newKeyPair = createKeyPair()
        savePrivateKey(newKeyPair)
        val newCert = createCertificate(newKeyPair)
        saveCertificate(newCert)

        when(status) {
            "failed validation" -> logger.info("Previous certificate or key failed validation")
            "failed load" -> logger.info("Failed to load previous system key or certificate")
            "missing files" -> logger.info("Some files were missing, moving existing to old")
        }

        logger.info("New system key pair and certificate created")
        return newKeyPair to newCert
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

     private fun validateCertificate(cert: X509Certificate, keyPair: KeyPair): Boolean {
         return checkDate(cert) && checkSelfSigned(cert) && checkMatchesPrivateKey(cert, keyPair)
     }

     private fun checkDate(cert: X509Certificate): Boolean = try {
         cert.checkValidity()
         true
     } catch (e: Exception) {
         false
     }
     private fun checkSelfSigned(cert: X509Certificate): Boolean = try {
         cert.verify(cert.publicKey)
         true
     } catch (e: Exception) {
         false
     }

     private fun checkMatchesPrivateKey(cert: X509Certificate, keyPair: KeyPair): Boolean {
         val test = "test".toByteArray()
         val signature = cryptoProvider.sign(test, keyPair.private)
         return cryptoProvider.verify(test, signature, cert.publicKey)
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