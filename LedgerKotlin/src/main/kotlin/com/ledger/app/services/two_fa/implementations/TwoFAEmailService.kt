package com.ledger.app.services.two_fa.implementations

import com.ledger.app.services.two_fa.TwoFAService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Service
class TwoFAEmailService (
    private val emailSender: EmailSender,
    @Qualifier("twoFATestFlag") private val test: Boolean
) : TwoFAService {

    companion object {
        data class VerificationCode(val code: String, val timestamp: Long, val service: String)
        private const val EXPIRATION_TIME_MS = 5 * 60 * 1000
    }

    private val logger = ColorLogger("2FAService", RGB.CYAN_SOFT, LogLevel.DEBUG)

    private val codes = ConcurrentHashMap<String, VerificationCode>()

    override fun sendCode(email: String, service: String): Boolean {
        if (test) {
            logger.debug("2FA is in TEST mode, code not actually sent")
            return true
        }

        val code = generateCode()
        val subject = "Your Verification Code"
        val body = createHtmlEmail(code, EXPIRATION_TIME_MS / 60 / 1000)

        if (emailSender.send(email, subject, body)) {
            codes[email] = VerificationCode(code, System.currentTimeMillis(), service)
            logger.debug("Sent code $code to email $email")
            return true
        }
        return false
    }

    private fun createHtmlEmail(code: String, expirationMinutes: Int): String {
        return """
        <html>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
            <div style="text-align: center; background-color: #f8f9fa; padding: 30px; border-radius: 10px;">
                <h2 style="color: #333; margin-bottom: 20px;">Verification Code</h2>
                <p style="color: #666; margin-bottom: 25px;">Please use this code to complete your verification:</p>
                <div style="background-color: #007bff; color: white; padding: 15px 25px; border-radius: 5px; display: inline-block; margin: 20px 0;">
                    <span style="font-size: 28px; font-weight: bold; letter-spacing: 3px;">$code</span>
                </div>
                <p style="color: #dc3545; font-weight: bold;">Expires in $expirationMinutes minutes</p>
                <p style="color: #6c757d; font-size: 14px; margin-top: 30px;">
                    If you didn't request this code, please ignore this email.
                </p>
            </div>
        </body>
        </html>
    """.trimIndent()
    }

    override fun verifyCode(email: String, code: String, service: String): Boolean {
        if (test) {
            logger.debug("2FA is in TEST mode, code not actually verified")
            return true
        }

        val entry = codes[email] ?: return false
        val now = System.currentTimeMillis()

        return if (now - entry.timestamp <= EXPIRATION_TIME_MS && entry.code == code && entry.service == service) {
            logger.debug("Code verified")
            codes.remove(email)
            true
        } else {
            logger.debug("Code is expired or incorrect")
            codes.remove(email)
            false
        }
    }

    private fun generateCode() = String.format("%06d", Random.nextInt(1000000))
}