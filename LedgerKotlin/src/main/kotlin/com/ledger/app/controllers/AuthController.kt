package com.ledger.app.controllers

import com.ledger.app.dtos.*
import com.ledger.app.models.AuthenticatedUser
import com.ledger.app.models.SimpleAuthResult
import com.ledger.app.services.auth.AuthService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.Rgb
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    private val logger = ColorLogger("AuthController", Rgb(50, 200, 50), LogLevel.DEBUG)

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<SimpleAuthResult> {
        logger.info("Register request for email: ${request.email}")
        return try {
            val result = authService.registerUser(
                request.email,
                request.passwordHash,
                request.fullName
            )
            logger.info("Registration successful for: ${request.email}")
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            logger.warn("Registration failed: ${e.message}")
            ResponseEntity.badRequest().body(null)
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<SimpleAuthResult> {
        logger.info("Login request for email: ${request.email}")
        return try {
            val result = authService.loginUser(request.email, request.passwordHash)
            logger.info("Login successful for: ${request.email}")
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            logger.warn("Login failed: ${e.message}")
            ResponseEntity.badRequest().body(null)
        }
    }

    @PostMapping("/verify")
    fun verifyCode(@RequestBody request: VerifyCodeRequest): ResponseEntity<AuthenticatedUser> {
        logger.info("Verification request for email: ${request.email}")
        return try {
            val result = authService.verifyCodeAndGetToken(request.email, request.code)
            logger.info("Verification successful for: ${request.email}")
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            logger.warn("Verification failed: ${e.message}")
            ResponseEntity.badRequest().body(null)
        }
    }

    @PostMapping("/validate")
    fun validateToken(@RequestBody request: ValidateTokenRequest): ResponseEntity<AuthenticatedUser?> {
        logger.debug("Token validation request")
        val user = authService.validateToken(request.token)
        return if (user != null) {
            logger.debug("Token valid for: ${user.email}")
            ResponseEntity.ok(user)
        } else {
            logger.warn("Invalid token provided")
            ResponseEntity.ok(null)
        }
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: ValidateTokenRequest): ResponseEntity<String> {
        logger.info("Logout request")
        authService.logoutUser(request.token)
        return ResponseEntity.ok("Logged out successfully")
    }
}
