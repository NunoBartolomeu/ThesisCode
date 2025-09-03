package com.ledger.app.repositories.pki.implementations

import com.ledger.app.repositories.pki.PKIRepo
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyPair
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.time.LocalDateTime

@Repository
class PKIRepoFileSystem(
    @Value("\${pki.base.path:./}") private val basePath: String
) : PKIRepo {
    private val systemDir = Paths.get(basePath, "system_pki")
    private val usersDir = Paths.get(basePath, "users_pki")
    private val keyPath = systemDir.resolve("system_private_key.pem")
    private val certPath = systemDir.resolve("system_certificate.crt")
    private val oldDir = systemDir.resolve("old")

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    override fun saveSystemKeyAndCertificate(keyPair: KeyPair, certificate: Certificate) {
        moveToOld()
        Files.createDirectories(systemDir)

        PemWriter(Files.newBufferedWriter(keyPath)).use {
            it.writeObject(PemObject("PRIVATE KEY", keyPair.private.encoded))
        }
        PemWriter(Files.newBufferedWriter(certPath)).use {
            it.writeObject(PemObject("CERTIFICATE", certificate.encoded))
        }
    }

    override fun saveUserCertificate(userId: String, certificate: Certificate) {
        Files.createDirectories(usersDir)
        val userCertPath = usersDir.resolve("user_${userId}_certificate.crt")

        PemWriter(Files.newBufferedWriter(userCertPath)).use {
            it.writeObject(PemObject("CERTIFICATE", certificate.encoded))
        }
    }

    override fun loadSystemKeyPair(): KeyPair? {
        return try {
            Files.newBufferedReader(keyPath).use { reader ->
                PEMParser(reader).use { parser ->
                    val converter = JcaPEMKeyConverter().setProvider("BC")
                    val obj = parser.readObject()
                    when (obj) {
                        is PEMKeyPair -> converter.getKeyPair(obj)
                        is PrivateKeyInfo -> {
                            val privateKey = converter.getPrivateKey(obj)
                            val publicKey = loadSystemCertificate()?.publicKey
                                ?: throw IllegalStateException("Cannot load public key from certificate")
                            KeyPair(publicKey, privateKey)
                        }
                        else -> throw IllegalArgumentException("Unsupported key format")
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun loadSystemCertificate(): X509Certificate? {
        return try {
            Files.newBufferedReader(certPath).use { reader ->
                PEMParser(reader).use { parser ->
                    val holder = parser.readObject() as X509CertificateHolder
                    JcaX509CertificateConverter().setProvider("BC").getCertificate(holder)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun loadUserCertificate(userId: String): X509Certificate? {
        return try {
            val userCertPath = usersDir.resolve("user_${userId}_certificate.crt")
            Files.newBufferedReader(userCertPath).use { reader ->
                PEMParser(reader).use { parser ->
                    val holder = parser.readObject() as X509CertificateHolder
                    JcaX509CertificateConverter().setProvider("BC").getCertificate(holder)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun systemFilesExist(): Boolean {
        return Files.exists(keyPath) && Files.exists(certPath)
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