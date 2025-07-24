package com.ledger.app.services.auth

import com.ledger.app.models.Token
import com.ledger.app.models.User

interface AuthService {
    fun registerUser(email: String, passwordHash: String, fullName: String): User
    fun loginUser(email: String, passwordHash: String): User
    fun verifyCodeAndGetToken(email: String, code: String): Token
    fun logoutUser(userId: String)
    fun validateToken(token: String): Boolean
    fun getUserInfo(userId: String): User?
}