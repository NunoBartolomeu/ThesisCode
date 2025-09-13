package com.ledger.app.repositories.auth.implementations

import com.ledger.app.models.User
import com.ledger.app.repositories.auth.AuthRepo
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
class AuthRepoJDBC(
    private val dataSource: DataSource
) : AuthRepo {

    init {
        createTableIfNotExists()
    }

    private fun createTableIfNotExists() {
        val sql = """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(255) PRIMARY KEY,
                email VARCHAR(255) UNIQUE NOT NULL,
                hashed_password VARCHAR(255) NOT NULL,
                full_name VARCHAR(255) NOT NULL
            )
        """.trimIndent()

        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(sql)
            }
        }
    }

    override fun saveUser(user: User): Boolean {
        val sql = """
            INSERT INTO users (id, email, hashed_password, full_name)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, user.id)
                    stmt.setString(2, user.email)
                    stmt.setString(3, user.hashedPassword)
                    stmt.setString(4, user.fullName)
                    stmt.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            println(e)
            false
        }
    }

    override fun getUser(userId: String): User? {
        val sql = "SELECT * FROM users WHERE id = ?"

        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, userId)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            User(
                                id = rs.getString("id"),
                                email = rs.getString("email"),
                                hashedPassword = rs.getString("hashed_password"),
                                fullName = rs.getString("full_name")
                            )
                        } else null
                    }
                }
            }
        } catch (e: Exception) {
            println(e)
            null
        }
    }

    override fun getUserByEmail(email: String): User? {
        val sql = "SELECT * FROM users WHERE email = ?"

        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, email)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            User(
                                id = rs.getString("id"),
                                email = rs.getString("email"),
                                hashedPassword = rs.getString("hashed_password"),
                                fullName = rs.getString("full_name")
                            )
                        } else null
                    }
                }
            }
        } catch (e: Exception) {
            println(e)
            null
        }
    }

    override fun getAllUsers(): List<User> {
        val sql = "SELECT * FROM users"
        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.executeQuery().use { rs ->
                        val users = mutableListOf<User>()
                        while (rs.next()) {
                            users.add(
                                User(
                                    id = rs.getString("id"),
                                    email = rs.getString("email"),
                                    hashedPassword = rs.getString("hashed_password"),
                                    fullName = rs.getString("full_name")
                                )
                            )
                        }
                        users
                    }
                }
            }
        } catch (e: Exception) {
            println(e)
            emptyList()
        }
    }

    override fun updateUser(user: User): Boolean {
        val sql = """
            UPDATE users 
            SET email = ?, hashed_password = ?, full_name = ?
            WHERE id = ?
        """.trimIndent()

        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, user.email)
                    stmt.setString(2, user.hashedPassword)
                    stmt.setString(3, user.fullName)
                    stmt.setString(4, user.id)
                    stmt.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            println(e)
            false
        }
    }
}