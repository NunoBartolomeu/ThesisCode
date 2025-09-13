package com.ledger.app.dtos

data class LoginRequest(val email: String, val passwordHash: String)
data class RegisterRequest(val email: String, val passwordHash: String, val fullName: String)
data class VerifyCodeRequest(val email: String, val code: String)
data class ValidateTokenRequest(val token: String)
data class SimpleAuthResult(val userId: String, val email: String, val fullName: String)
data class SimpleUserNameAndEmail(val email: String, val fullName: String)
data class AccessTokenResult(val accessToken: String, val expiresAt: Long)