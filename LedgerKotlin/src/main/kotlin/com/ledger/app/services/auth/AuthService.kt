package com.ledger.app.services.auth

import com.ledger.app.models.AuthenticatedUser
import com.ledger.app.models.SimpleAuthResult
import com.ledger.app.models.User

interface AuthService {
    fun registerUser(email: String, passwordHash: ByteArray, fullName: String): SimpleAuthResult
    fun loginUser(email: String, passwordHash: ByteArray): SimpleAuthResult
    fun verifyCodeAndGetToken(email: String, code: String): AuthenticatedUser
    fun logoutUser(userId: String)
    fun validateToken(token: String): AuthenticatedUser?
    fun getUserInfo(userId: String): User?
}