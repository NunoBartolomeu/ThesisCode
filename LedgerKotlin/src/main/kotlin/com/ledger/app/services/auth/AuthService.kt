package com.ledger.app.services.auth

import com.ledger.app.models.Token
import com.ledger.app.models.User

interface AuthService {
    fun registerUser(email: String, passwordHash: String, fullName: String): User
    fun loginUser(email: String, passwordHash: String): User
    fun logoutUser(userId: String)
    fun verifyCodeAndGetToken(email: String, code: String): Token
    fun validateToken(token: String): Boolean
    fun getUserInfo(userId: String): User?
    fun getAllUsers(): List<User>
}