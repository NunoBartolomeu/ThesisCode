package com.ledger.app.configurations.security

import com.ledger.app.repositories.auth.AuthRepo
import com.ledger.app.services.auth.implementations.AuthServiceSpring
import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB
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

    private val logger = ColorLogger("JwtAuthenticationFilter", RGB.PINK, LogLevel.DEBUG)

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
            val token = authHeader.removePrefix("Bearer ").trim()
            try {
                val email = jwtUtil.validateAndGetSubject(token)

                if (email != null) {
                    // Get user by email instead of userId
                    val user = repo.getUserByEmail(email)
                    if (user != null) {
                        val auth = UsernamePasswordAuthenticationToken(user, null, emptyList())
                        SecurityContextHolder.getContext().authentication = auth
                    }
                }
            } catch (e: Exception) {
                logger.error(e.message!!)
            }
        }

        filterChain.doFilter(request, response)

        if (response.status >= 400) {
            logger.error("Request BOUNCED with status ${response.status}: $method $requestUri")
        } else {
            logger.debug("Request processed successfully with status ${response.status}: $method $requestUri")
        }
    }
}