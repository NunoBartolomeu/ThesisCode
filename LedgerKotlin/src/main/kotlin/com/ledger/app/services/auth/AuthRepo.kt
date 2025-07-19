package com.ledger.app.services.auth

import com.ledger.app.models.User

interface AuthRepo {
    fun saveUser(user: User): Boolean
    fun getUser(userId: String): User?
    fun getUserByEmail(email: String): User?
    fun updateUser(user: User): Boolean
    fun verifyEmail(userId: String): Boolean
}