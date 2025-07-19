package com.ledger.app.services.two_fa.implementations

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.springframework.stereotype.Component
import java.util.Properties

@Component
class EmailSender {
    //email: ledgerkotlin@gmail.com
    //email-password: SuperSecrect2FA
    //app-password: vnxw swdo pbcn grov

    private val username = "ledgerkotlin@gmail.com"
    private val password = "vnxw swdo pbcn grov"
    private val host = "smtp.gmail.com"
    private val port = "587"

    private val props = Properties().apply {
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", host)
        put("mail.smtp.port", port)
    }

    fun send(toEmail: String, subject: String, body: String): Boolean {
        return try {
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(username, password)
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                setSubject(subject)
                setText(body)
            }

            Transport.send(message)
            true
        } catch (e: MessagingException) {
            println("Failed to send email: ${e.message}")
            false
        }
    }
}