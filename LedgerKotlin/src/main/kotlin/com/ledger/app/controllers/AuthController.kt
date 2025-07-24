package com.ledger.app.controllers

import com.ledger.app.dtos.*
import com.ledger.app.services.auth.AuthService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    private val logger = ColorLogger("AuthController", RGB.GREEN_DARK, LogLevel.DEBUG)

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
}
