package com.ledger.app.services.pki

import java.security.KeyPair
import java.security.PublicKey
import java.security.cert.Certificate

interface PublicKeyInfrastructureService {
    fun getSystemKeyPair(): KeyPair
    fun getSystemCertificate(): Certificate
    fun getUserCertificate(userId: String): Certificate?
    fun associatePublicKeyToUser(userId: String, publicKey: PublicKey): Certificate

    fun verifyCertificate(userId: String, certificate: Certificate): Boolean
}