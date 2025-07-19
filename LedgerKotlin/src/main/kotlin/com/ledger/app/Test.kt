package com.ledger.app

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.*
import kotlin.random.Random

/**
 * Simple email sender component for 2FA verification
 */
class EmailSender(
    private val username: String,
    private val password: String,
    private val host: String = "smtp.gmail.com",
    private val port: String = "587"
) {
    private val props = Properties().apply {
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", host)
        put("mail.smtp.port", port)
    }

    /**
     * Generate a random 6-digit code for 2FA
     */
    fun generateVerificationCode(): String {
        return String.format("%06d", Random.nextInt(1000000))
    }

    /**
     * Send a verification code to the specified email address
     * @param toEmail recipient's email address
     * @param code verification code to send
     * @return true if the email was sent successfully, false otherwise
     */
    fun sendVerificationCode(toEmail: String, code: String): Boolean {
        return try {
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            }).apply {
                debug = true
            }

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "Your Verification Code"
                setText("Your verification code is: $code\n\nThis code will expire in 10 minutes.")
            }

            Transport.send(message)

            true
        } catch (e: MessagingException) {
            println("Failed to send email: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

//email: ledgerkotlin@gmail.com
//email-password: SuperSecrect2FA
//app-password: vnxw swdo pbcn grov

/**
 * Main application for testing the com.ledger.app.EmailSender component
 */
fun main() {
    val scanner = Scanner(System.`in`)

    // Get sender email credentials
    println("== Email 2FA Test com.ledger.app.Application ==")
    println("\nNOTE: For Gmail, you need to use an App Password.")
    println("You can create one at: https://myaccount.google.com/apppasswords")

    print("\nEnter sender email address: ")
    val senderEmail = "ledgerkotlin@gmail.com"//scanner.nextLine()

    print("Enter email password or app password: ")
    val password = "vnxw swdo pbcn grov"//scanner.nextLine()

    // Create email sender
    val emailSender = EmailSender(senderEmail, password)

    // Get recipient email
    print("\nEnter recipient email address: ")
    val recipientEmail = scanner.nextLine()

    // Generate and send verification code
    val verificationCode = emailSender.generateVerificationCode()
    println("\nSending verification code to $recipientEmail...")

    val success = emailSender.sendVerificationCode(recipientEmail, verificationCode)
    if (success) {
        println("Verification code sent successfully!")

        // Verify the code
        print("\nEnter the verification code you received: ")
        val enteredCode = scanner.nextLine()

        if (enteredCode == verificationCode) {
            println("\n✅ Verification successful!")
        } else {
            println("\n❌ Verification failed. Incorrect code.")
        }
    } else {
        println("Failed to send verification code. Please check your credentials and try again.")
    }
}