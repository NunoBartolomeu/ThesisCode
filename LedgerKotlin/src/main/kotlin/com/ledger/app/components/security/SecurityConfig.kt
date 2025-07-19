package com.ledger.app.components.security

import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.Rgb
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    private val logger = ColorLogger("SecurityConfig", Rgb(255, 165, 0), LogLevel.DEBUG)

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        logger.info("Configuring security filter chain...")

        http
            .cors {
                logger.debug("CORS configuration applied")
            }
            .csrf {
                it.disable()
                logger.debug("CSRF disabled for stateless API")
            }
            .authorizeHttpRequests { authz ->
                logger.info("Configuring request authorization...")
                authz.requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/auth/verify"
                ).permitAll()
                    .anyRequest().authenticated()

                logger.debug("Permitted endpoints: /auth/register, /auth/login, /auth/verify")
                logger.debug("All other endpoints require authentication")
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        logger.info("Security filter chain configured successfully")
        return http.build()
    }

    @Bean
    fun corsFilter(): CorsFilter {
        logger.info("Configuring CORS filter...")

        val config = CorsConfiguration()
        config.allowCredentials = true
        config.addAllowedOrigin("http://localhost:3000")
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)

        logger.info("CORS filter configured - allowing localhost:3000 with credentials")
        logger.debug("CORS config: credentials=true, origin=http://localhost:3000, headers=*, methods=*")

        return CorsFilter(source)
    }
}