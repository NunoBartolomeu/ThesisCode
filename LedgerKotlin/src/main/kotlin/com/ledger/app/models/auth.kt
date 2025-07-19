package com.ledger.app.models

data class User(
    val id: String,
    val email: String,
    val hashedPassword: String,
    val fullName: String,
    val emailVerified: Boolean = false
)

data class SimpleAuthResult(
    val email: String,
    val fullName: String,
    val needsVerification: Boolean
)

data class AuthenticatedUser(
    val email: String,
    val fullName: String,
    val accessToken: String,
    val expiresAt: Long
)

data class TwoFactorCode(
    val email: String,
    val code: String,
    val expiresAt: Long
)