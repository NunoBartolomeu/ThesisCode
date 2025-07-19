package com.ledger.app.components.security

import com.ledger.app.services.auth.AuthRepo
import com.ledger.app.services.auth.implementations.AuthServiceSpring
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.Rgb
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: AuthServiceSpring.Companion.JwtUtil,
    private val repo: AuthRepo
) : OncePerRequestFilter() {

    private val logger = ColorLogger("JwtAuthenticationFilter", Rgb(255, 100, 100), LogLevel.DEBUG)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestUri = request.requestURI
        val method = request.method

        logger.debug("Processing request: $method $requestUri")

        val authHeader = request.getHeader("Authorization")

        if (authHeader?.startsWith("Bearer ") == true) {
            logger.debug("Authorization header found with Bearer token")

            val token = authHeader.removePrefix("Bearer ").trim()
            logger.debug("Extracted JWT token (length: ${token.length})")

            try {
                // Now JWT tokens contain email, not userId
                val email = jwtUtil.validateAndGetSubject(token)

                if (email != null) {
                    logger.info("JWT token valid for email: $email")

                    // Get user by email instead of userId
                    val user = repo.getUserByEmail(email)
                    if (user != null) {
                        logger.info("User found in database for email: $email")
                        val auth = UsernamePasswordAuthenticationToken(user, null, emptyList())
                        SecurityContextHolder.getContext().authentication = auth
                        logger.info("Authentication successful for $email on $method $requestUri")
                    } else {
                        logger.warn("⚠️  JWT token valid but user not found in database for email: $email")
                        logger.warn("⚠️  Request will be BOUNCED: $method $requestUri")
                    }
                } else {
                    logger.warn("⚠️  JWT token validation failed - invalid or expired token")
                    logger.warn("⚠️  Request will be BOUNCED: $method $requestUri")
                }
            } catch (e: Exception) {
                logger.error("⚠️  JWT token processing error: ${e.message}")
                logger.error("⚠️  Request will be BOUNCED: $method $requestUri")
            }
        } else {
            // Check if this is a permitted endpoint
            val permittedEndpoints = listOf("/auth/register", "/auth/login", "/auth/verify")
            val isPermitted = permittedEndpoints.any { requestUri.startsWith(it) }

            if (isPermitted) {
                logger.debug("No Authorization header - but endpoint is permitted: $requestUri")
            } else {
                logger.warn("⚠️  No Authorization header found for protected endpoint")
                logger.warn("⚠️  Request will be BOUNCED: $method $requestUri")
            }
        }

        filterChain.doFilter(request, response)

        // Log the response status after processing
        if (response.status >= 400) {
            logger.error("🚫 Request BOUNCED with status ${response.status}: $method $requestUri")
        } else {
            logger.debug("✅ Request processed successfully with status ${response.status}: $method $requestUri")
        }
    }
}