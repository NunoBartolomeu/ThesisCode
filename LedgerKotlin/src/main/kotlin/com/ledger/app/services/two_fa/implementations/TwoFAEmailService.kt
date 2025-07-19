package com.ledger.app.services.two_fa.implementations

import com.ledger.app.services.two_fa.TwoFAService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.Rgb
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Service
class TwoFAEmailService (
    private val emailSender: EmailSender,
    @Value("\${twofactor.test:false}") private val test: Boolean
) : TwoFAService {

    companion object {
        data class VerificationCode(val code: String, val timestamp: Long)
        private const val EXPIRATION_TIME_MS = 5 * 60 * 1000
    }

    private val logger = ColorLogger("2FAService", Rgb(50, 50, 150), LogLevel.DEBUG)

    private val codeStorage = ConcurrentHashMap<String, VerificationCode>()

    override fun sendCode(email: String): Boolean {
        if (test) {
            logger.debug("2FA is in TEST mode")
            return true
        }

        val code = generateCode()
        val subject = "Your Verification Code"
        val body = "Your verification code is: $code\n\nThis code will expire in ${EXPIRATION_TIME_MS / 60 / 1000} minutes."

        if (emailSender.send(email, subject, body)) {
            codeStorage[email] = VerificationCode(code, System.currentTimeMillis())
            logger.debug("Sent code $code to email $email")
            return true
        }
        return false
    }

    override fun verifyCode(email: String, code: String): Boolean {
        val entry = codeStorage[email] ?: return false
        val now = System.currentTimeMillis()

        return if (now - entry.timestamp <= EXPIRATION_TIME_MS && entry.code == code) {
            logger.debug("Code verified")
            codeStorage.remove(email)
            true
        } else {
            logger.debug("Code is expired or incorrect")
            codeStorage.remove(email) // Expired or incorrect
            false
        }
    }

    private fun generateCode() = String.format("%06d", Random.nextInt(1000000))
}