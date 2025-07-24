package com.ledger.app.models

data class User(
    val id: String,
    val email: String,
    val hashedPassword: String,
    val fullName: String,
    val emailVerified: Boolean = false
)

data class Token(
    val accessToken: String,
    val expiresAt: Long
)