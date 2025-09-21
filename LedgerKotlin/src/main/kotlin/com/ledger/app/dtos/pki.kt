package com.ledger.app.dtos

// Request DTOs
data class CreateCertificateRequest(
    val userId: String,
    val publicKey: String, // Hex encoded public key
    val algorithm: String
)

data class VerifyCertificateRequest(
    val userId: String,
    val certificate: String // Base64 encoded certificate
)

// Response DTOs
data class CertificateDetailsDTO(
    val certificateBase64: String,
    val serialNumber: String,
    val issuer: String,
    val subject: String,
    val validFrom: String,
    val validTo: String,
    val publicKeyAlgorithm: String,
    val signatureAlgorithm: String
)

data class CertificateResponse(
    val certificate: CertificateDetailsDTO,
    val userId: String
)

data class CertificateVerificationResult(
    val isValid: Boolean
)