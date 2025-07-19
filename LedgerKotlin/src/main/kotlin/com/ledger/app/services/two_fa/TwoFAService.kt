package com.ledger.app.services.two_fa

interface TwoFAService {
    fun sendCode(email: String): Boolean
    fun verifyCode(email: String, code: String): Boolean
}