package com.ledger.app.repositories.auth.implementations

import com.ledger.app.models.User
import com.ledger.app.repositories.auth.AuthRepo
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

//@Repository
class AuthRepoMemory: AuthRepo {
    private val users = ConcurrentHashMap<String, User>()
    private val emails = ConcurrentHashMap<String, String>() // email -> userId

    override fun saveUser(user: User): Boolean {
        users[user.id] = user
        emails[user.email] = user.id
        return true
    }

    override fun getUser(userId: String): User? = users[userId]

    override fun getUserByEmail(email: String): User? = emails[email]?.let { users[it] }

    override fun updateUser(user: User): Boolean = saveUser(user)

    override fun verifyEmail(userId: String): Boolean {
        val newUser = users[userId]
        if (newUser != null) {
            newUser.copy(emailVerified = true)
            saveUser(newUser)
            return true
        }
        return false
    }
}