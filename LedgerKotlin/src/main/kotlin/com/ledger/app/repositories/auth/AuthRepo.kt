package com.ledger.app.repositories.auth

import com.ledger.app.models.User

interface AuthRepo {
    fun saveUser(user: User): Boolean
    fun getUser(userId: String): User?
    fun getUserByEmail(email: String): User?
    fun getAllUsers(): List<User>
    fun updateUser(user: User): Boolean
    fun verifyEmail(userId: String): Boolean
}