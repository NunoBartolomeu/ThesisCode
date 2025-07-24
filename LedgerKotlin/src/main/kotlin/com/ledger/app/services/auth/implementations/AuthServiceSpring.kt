package com.ledger.app.services.auth.implementations

import com.ledger.app.models.Token
import com.ledger.app.models.User
import com.ledger.app.services.auth.AuthRepo
import com.ledger.app.services.auth.AuthService
import com.ledger.app.services.files.FilesService
import com.ledger.app.services.ledger.LedgerService
import com.ledger.app.services.two_fa.TwoFAService
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.CryptoProvider
import com.ledger.app.utils.HashProvider
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.context.annotation.Lazy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class AuthServiceSpring(
    hashProvider: HashProvider,
    cryptoProvider: CryptoProvider,
    private val repo: AuthRepo,
    private val ledgerService: LedgerService,
    @Lazy private val filesService: FilesService,
    private val twoFAService: TwoFAService,
    private val passwordEncoder: PasswordEncoder,
) : AuthService {

    private val AUTH_SYSTEM = "auth_service"
    private val AUTH_LEDGER = "auth_ledger"

    private val logger = ColorLogger("AuthService", RGB.GREEN_LIME, LogLevel.DEBUG)
    private val activeTokens = ConcurrentHashMap<String, String>()

    companion object {
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

    init {
        ledgerService.createLedger(AUTH_LEDGER, 2, hashProvider.algorithm, cryptoProvider.algorithm)
    }

    private fun generateId(): String = UUID.randomUUID().toString()

    override fun registerUser(email: String, passwordHash: String, fullName: String): User {
        if (repo.getUserByEmail(email) != null) {
            logger.error("Email $email is already registered to a user")
            throw IllegalArgumentException("Email already in use")
        }
        val encodedPassword = passwordEncoder.encode(passwordHash)
        val user = User(id = generateId(), email = email, hashedPassword = encodedPassword, fullName = fullName, emailVerified = false)

        if (!repo.saveUser(user)) {
            logger.error("Repository failed to save user $fullName")
            throw IllegalArgumentException("Invalid credentials")
        }

        twoFAService.sendCode(email)

        logger.info("Registered user ${user.fullName}, needing verification")
        ledgerService.logSystemEvent(AUTH_LEDGER, AUTH_SYSTEM, user.id, "Registered new user ${user.id}, needs verification")

        filesService.initiateLedgerForUser(user.id)

        return user
    }

    override fun loginUser(email: String, passwordHash: String): User {
        val user = repo.getUserByEmail(email)

        if (user == null) {
            logger.error("Email $email is not associated with a user")
            throw IllegalArgumentException("Invalid credentials")
        }

        if (!passwordEncoder.matches(passwordHash, user.hashedPassword)) {
            logger.error("Password authentication failed for $email")
            throw IllegalArgumentException("Invalid credentials")
        }

        twoFAService.sendCode(email)

        logger.info("Authenticated user ${user.fullName}")
        ledgerService.logSystemEvent(AUTH_LEDGER, AUTH_SYSTEM, user.id, "Authenticated user ${user.fullName}")
        return user
    }

    override fun verifyCodeAndGetToken(email: String, code: String): Token {
        if (!twoFAService.verifyCode(email, code)) {
            logger.error("Invalid or expired verification code for $email")
            throw IllegalArgumentException("Invalid or expired verification code for $email")
        }

        val user = repo.getUserByEmail(email)?: throw IllegalArgumentException("User not found")

        if (!user.emailVerified) {
            repo.verifyEmail(user.id)
            logger.info("Email verified for user ${user.fullName}")
        }

        val accessToken = jwtUtil.createAccessToken(email)
        val expiresAt = Instant.now().plusSeconds(24 * 60 * 60).epochSecond

        activeTokens[accessToken] = email

        logger.info("User ${user.fullName} authenticated successfully")
        ledgerService.logSystemEvent(AUTH_LEDGER, AUTH_SYSTEM, user.id, "User ${user.fullName} authenticated successfully")

        return Token(
            accessToken = accessToken,
            expiresAt = expiresAt
        )
    }

    override fun validateToken(token: String): Boolean {
        val email = activeTokens[token] ?: return false

        val tokenEmail = jwtUtil.validateAndGetSubject(token)
        if (tokenEmail != email) {
            activeTokens.remove(token)
            return false
        }

        val user = repo.getUserByEmail(email)?: return false
        logger.debug("Token validated for ${user.fullName}")

        return true
    }

    override fun getUserInfo(userId: String): User? {
        return repo.getUser(userId)
    }

    override fun logoutUser(userId: String) {
        val user = repo.getUser(userId)
        if (user?.email != null) {
            logger.info("User logged out: ${user.email}")
        }
    }
}