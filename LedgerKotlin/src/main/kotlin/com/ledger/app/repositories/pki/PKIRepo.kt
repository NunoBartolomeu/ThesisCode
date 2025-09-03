package com.ledger.app.repositories.pki

import java.security.KeyPair
import java.security.cert.Certificate

interface PKIRepo {
    fun saveSystemKeyAndCertificate(keyPair: KeyPair, certificate: Certificate)
    fun saveUserCertificate(userId: String, certificate: Certificate)

    fun loadSystemKeyPair(): KeyPair?
    fun loadSystemCertificate(): Certificate?
    fun loadUserCertificate(userId: String): Certificate?

    fun systemFilesExist(): Boolean
}