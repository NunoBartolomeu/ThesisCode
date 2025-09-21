package com.ledger.app.controllers

import com.ledger.app.dtos.*
import com.ledger.app.services.pki.PublicKeyInfrastructureService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.RGB
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.KeyFactory
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@RestController
@RequestMapping("/certificate")
class PKIController(
    private val pkiService: PublicKeyInfrastructureService
) {
    @Value("\${app.logLevel:INFO}")
    private lateinit var logLevelStr: String
    private lateinit var logger: ColorLogger

    @PostConstruct
    fun initialize() {
        logger = ColorLogger("CertificateController", RGB.PURPLE, logLevelStr)
    }

    @PostMapping("/create")
    fun createCertificate(@RequestBody request: CreateCertificateRequest): ResponseEntity<CertificateResponse> {
        logger.debug("Create certificate request for userId: ${request.userId}")
        return try {
            logger.debug("Request: $request")
            logger.debug("Public Key: ${request.publicKey}")
            logger.debug("Algorithm: ${request.algorithm}")
            val certificate = pkiService.associatePublicKeyToUser(request.userId, request.publicKey, request.algorithm)

            logger.info("Certificate created successfully for userId: ${request.userId}")
            ResponseEntity.ok(CertificateResponse(extractCertificateDetails(certificate), request.userId))
        } catch (e: Exception) {
            logger.warn("Certificate creation failed for userId: ${request.userId}, error: ${e.message}")
            ResponseEntity.badRequest().body(null)
        }
    }

    @PostMapping("/verify")
    fun verifyCertificate(@RequestBody request: VerifyCertificateRequest): ResponseEntity<CertificateVerificationResult> {
        logger.debug("Verify certificate request for userId: ${request.userId}")
        return try {
            val certificate = decodeCertificate(request.certificate)
            val isValid = pkiService.verifyCertificate(request.userId, certificate)

            logger.info("Certificate verification for userId: ${request.userId}, result: $isValid")
            ResponseEntity.ok(CertificateVerificationResult(isValid))
        } catch (e: Exception) {
            logger.warn("Certificate verification failed for userId: ${request.userId}, error: ${e.message}")
            ResponseEntity.badRequest().body(CertificateVerificationResult(false))
        }
    }

    @GetMapping("/user/{userId}")
    fun getUserCertificate(@PathVariable userId: String): ResponseEntity<CertificateResponse> {
        logger.debug("Get certificate request for userId: $userId")
        return try {
            val certificate = pkiService.getUserCertificate(userId)
            if (certificate != null) {
                logger.info("Certificate retrieved successfully for userId: $userId")
                ResponseEntity.ok(CertificateResponse(extractCertificateDetails(certificate), userId))
            } else {
                logger.warn("Certificate not found for userId: $userId")
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.warn("Error retrieving certificate for userId: $userId, error: ${e.message}")
            ResponseEntity.badRequest().body(null)
        }
    }

    @GetMapping("/system")
    fun getSystemCertificate(): ResponseEntity<CertificateResponse> {
        logger.debug("Get system certificate request")
        return try {
            val certificate = pkiService.getSystemCertificate()
            logger.info("System certificate retrieved successfully")
            ResponseEntity.ok(CertificateResponse(extractCertificateDetails(certificate), "ledger_system"))
        } catch (e: Exception) {
            logger.warn("Error retrieving system certificate, error: ${e.message}")
            ResponseEntity.badRequest().body(null)
        }
    }

    private fun decodeCertificate(certificateBase64: String): Certificate {
        val certBytes = Base64.getDecoder().decode(certificateBase64)
        val certFactory = CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(certBytes.inputStream())
    }

    private fun extractCertificateDetails(certificate: Certificate): CertificateDetailsDTO {
        val x509Cert = certificate as java.security.cert.X509Certificate
        return CertificateDetailsDTO(
            certificateBase64 = Base64.getEncoder().encodeToString(x509Cert.encoded),
            serialNumber = x509Cert.serialNumber.toString(),
            issuer = x509Cert.issuerX500Principal.name,
            subject = x509Cert.subjectX500Principal.name,
            validFrom = x509Cert.notBefore.toInstant().toString(),
            validTo = x509Cert.notAfter.toInstant().toString(),
            publicKeyAlgorithm = x509Cert.publicKey.algorithm,
            signatureAlgorithm = x509Cert.sigAlgName
        )
    }
}