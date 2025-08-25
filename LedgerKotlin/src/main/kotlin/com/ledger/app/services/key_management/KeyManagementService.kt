package com.ledger.app.services.key_management

import java.security.KeyPair

interface KeyManagementService {
    fun getSystemKeyPair(): KeyPair

    //fun associatePublicKeyToUser(public key) We can make a certificate for the user using the systems certificate.
    // So they send the certificate/ have it here and is possible to check it.
}