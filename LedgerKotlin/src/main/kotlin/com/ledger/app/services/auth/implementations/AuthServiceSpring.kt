package com.ledger.app.services.auth.implementations

import com.ledger.app.models.Token
import com.ledger.app.models.User
import com.ledger.app.repositories.auth.AuthRepo
import com.ledger.app.services.auth.AuthService
import com.ledger.app.services.ledger.LedgerService
import com.ledger.app.services.two_fa.TwoFAService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.RGB
import com.ledger.app.utils.hash.HashProvider
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class AuthServiceSpring(
    private val repo: AuthRepo,
    private val ledgerService: LedgerService,
    private val twoFAService: TwoFAService,
    private val passwordEncoder: PasswordEncoder,
) : AuthService {

    companion object {
        private const val AUTH_LEDGER = "auth_ledger"
        private const val AUTH_SERVICE = "auth_service"

        @Component
        class JwtUtil {
            // 256‑bit HMAC secret; in prod load from env or vault
            private val secret = "test_secret_for_jwt_that_is_long_enough_for_hmac_sha256".toByteArray()
            private val tokenValidityInSeconds = 24 * 60 * 60L

            fun createAccessToken(email: String): String {
                val now = Instant.now()
                val expiryDate = now.plusSeconds(tokenValidityInSeconds)

                return Jwts.builder()
                    .setSubject(email)
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(expiryDate))
                    .signWith(Keys.hmacShaKeyFor(secret), SignatureAlgorithm.HS256)
                    .compact()
            }

            fun validateAndGetSubject(token: String): String? {
                return try {
                    val claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secret))
                        .build()
                        .parseClaimsJws(token)
                        .body
                    claims.subject
                } catch (_: Exception) {
                    null
                }
            }

            fun getExpirationTime(token: String): Long {
                return try {
                    val claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secret))
                        .build()
                        .parseClaimsJws(token)
                        .body

                    claims.expiration.time / 1000
                } catch (e: Exception) {
                    0L
                }
            }

            fun isTokenExpired(token: String): Boolean {
                return try {
                    val claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secret))
                        .build()
                        .parseClaimsJws(token)
                        .body

                    claims.expiration.before(Date())
                } catch (e: Exception) {
                    true
                }
            }
        }

        val jwtUtil = JwtUtil()
    }

    private val activeTokens = ConcurrentHashMap<String, String>()

    @Value("\${app.logLevel:INFO}")
    private lateinit var logLevelStr: String
    private lateinit var logger: ColorLogger

    @PostConstruct
    fun initialize() {
        logger = ColorLogger("AuthService", RGB.GREEN, logLevelStr)

        if (ledgerService.getLedger(AUTH_LEDGER) == null) {
            ledgerService.createLedger(AUTH_LEDGER, 3, HashProvider.getDefaultAlgorithm())
        }
    }

    private fun generateId(): String = UUID.randomUUID().toString()

    override fun registerUser(email: String, passwordHash: String, fullName: String): User {
        if (repo.getUserByEmail(email) != null) {
            logger.error("Email $email is already registered to a user")
            throw IllegalArgumentException("Email already in use")
        }

        val encodedPassword = passwordEncoder.encode(passwordHash)
        val user = User(id = generateId(), email = email, hashedPassword = encodedPassword, fullName = fullName)

        if (!repo.saveUser(user)) {
            logger.error("Repository failed to save user $fullName")
            throw IllegalArgumentException("Failed to save user")
        }

        twoFAService.sendCode(email, AUTH_SERVICE)

        ledgerService.logSystemEvent(AUTH_LEDGER, AUTH_SERVICE, user.id, "Registered new user ${user.id}")
        logger.info("Registered user ${user.fullName}, 2FA code sent")

        return user
    }

    override fun loginUser(email: String, passwordHash: String): User {
        val user = repo.getUserByEmail(email) ?: run {
                logger.error("Email $email is not associated with a user")
                throw IllegalArgumentException("Invalid credentials")
            }

        if (!passwordEncoder.matches(passwordHash, user.hashedPassword)) {
            logger.debug("Password gotten: ${passwordHash}")
            logger.error("Password authentication failed for $email")
            throw IllegalArgumentException("Invalid credentials")
        }

        twoFAService.sendCode(email, AUTH_SERVICE)

        logger.info("Password authenticated for user ${user.fullName}, 2FA code sent")
        ledgerService.logSystemEvent(AUTH_LEDGER, AUTH_SERVICE, user.id, "Password authenticated for user ${user.fullName}")

        return user
    }

    override fun logoutUser(userId: String) {
        val user = repo.getUser(userId)

        if (user?.email == null) {
            logger.warn("Attempted logout for non-existent user: $userId")
            return
        }

        // Remove all active tokens for this user
        val tokensToRemove = activeTokens.filterValues { it == user.email }.keys
        tokensToRemove.forEach { token ->
            activeTokens.remove(token)
        }

        logger.info("User logged out: ${user.email} (${tokensToRemove.size} tokens invalidated)")
        ledgerService.logSystemEvent(AUTH_LEDGER, AUTH_SERVICE, user.id, "User ${user.fullName} logged out")
    }

    override fun verifyCodeAndGetToken(email: String, code: String): Token {
        if (!twoFAService.verifyCode(email, code, AUTH_SERVICE)) {
            logger.error("Invalid or expired verification code for $email")
            throw IllegalStateException("Invalid or expired verification code")
        }

        val user = repo.getUserByEmail(email)
        if (user == null) {
            logger.error("User not found in repository for email: $email")
            throw IllegalArgumentException("User not found")
        }


        val accessToken = jwtUtil.createAccessToken(email)
        val expiresAt = Instant.now().plusSeconds(24 * 60 * 60).epochSecond

        activeTokens[accessToken] = email

        logger.info("User ${user.fullName} authenticated successfully with 2FA")
        ledgerService.logSystemEvent(AUTH_LEDGER, AUTH_SERVICE, user.id, "User ${user.fullName} authenticated successfully")

        return Token(
            accessToken = accessToken,
            expiresAt = expiresAt
        )
    }

    override fun validateToken(token: String): Boolean {
        val email = activeTokens[token] ?: return false

        // Validate JWT structure and signature
        val tokenEmail = jwtUtil.validateAndGetSubject(token)
        if (tokenEmail != email) {
            activeTokens.remove(token)
            logger.warn("Token validation failed: email mismatch")
            return false
        }

        // Check if token is expired
        if (jwtUtil.isTokenExpired(token)) {
            activeTokens.remove(token)
            logger.debug("Token expired and removed from active tokens")
            return false
        }

        // Verify user exists
        val user = repo.getUserByEmail(email) ?: run {
            activeTokens.remove(token)
            logger.warn("Token validation failed: user no longer exists")
            return false
        }

        logger.debug("Token validated for ${user.fullName}")
        return true
    }

    override fun getUserInfo(userId: String): User? {
        return repo.getUser(userId)
    }

    override fun getAllUsers(): List<User> {
        return repo.getAllUsers()
    }
}