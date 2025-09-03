package com.ledger.app.controllers

import com.ledger.app.dtos.*
import com.ledger.app.services.auth.AuthService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.RGB
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @Value("\${app.logLevel:INFO}")
    private lateinit var logLevelStr: String
    private lateinit var logger: ColorLogger

    @PostConstruct
    fun initialize() {
        logger = ColorLogger("AuthController", RGB.GREEN_DARK, logLevelStr)
    }

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<SimpleAuthResult> {
        logger.info("Register request for email: ${request.email}")
        return try {
            val user = authService.registerUser(request.email, request.passwordHash, request.fullName)
            logger.info("Registration successful for: ${request.email}")
            ResponseEntity.ok(SimpleAuthResult(user.email, user.fullName, true))
        } catch (e: IllegalArgumentException) {
            logger.warn("Registration failed: ${e.message}")
            ResponseEntity.badRequest().body(null)
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<SimpleAuthResult> {
        logger.info("Login request for email: ${request.email}")
        return try {
            val user = authService.loginUser(request.email, request.passwordHash)
            logger.info("Login successful for: ${request.email}")
            ResponseEntity.ok(SimpleAuthResult(user.email, user.fullName, true))
        } catch (e: IllegalArgumentException) {
            logger.warn("Login failed: ${e.message}")
            ResponseEntity.badRequest().body(null)
        }
    }

    @PostMapping("/verify")
    fun verifyCode(@RequestBody request: VerifyCodeRequest): ResponseEntity<AccessTokenResult> {
        logger.info("Verification request for email: ${request.email}")
        return try {
            val token = authService.verifyCodeAndGetToken(request.email, request.code)
            logger.info("Verification successful for: ${request.email}")
            ResponseEntity.ok(AccessTokenResult(token.accessToken, token.expiresAt))
        } catch (e: IllegalArgumentException) {
            logger.warn("Verification failed: ${e.message}")
            ResponseEntity.badRequest().body(null)
        }
    }

    @PostMapping("/validate")
    fun validateToken(@RequestBody request: ValidateTokenRequest): ResponseEntity<Boolean> {
        logger.debug("Token validation request")
        val valid = authService.validateToken(request.token)
        return if (valid) {
            logger.debug("Token is valid")
            ResponseEntity.ok(true)
        } else {
            logger.warn("Invalid token provided")
            ResponseEntity.ok(false)
        }
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: ValidateTokenRequest): ResponseEntity<String> {
        logger.info("Logout request")
        authService.logoutUser(request.token)
        return ResponseEntity.ok("Logged out successfully")
    }

    @GetMapping("/users")
    fun getAllUsers(@RequestBody request: HttpRequest): ResponseEntity<List<SimpleUserNameAndEmail>> {
        logger.info("Get All Users request")
        val users = authService.getAllUsers()
        return ResponseEntity.ok(users.map { SimpleUserNameAndEmail(it.email, it.fullName) })
    }
}
