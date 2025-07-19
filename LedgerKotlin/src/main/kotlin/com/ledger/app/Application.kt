package com.ledger.app

import com.ledger.app.utils.CryptoProvider
import com.ledger.app.utils.HashProvider
import com.ledger.app.utils.implementations.RSACryptoProvider
import com.ledger.app.utils.implementations.SHA256HashProvider
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Configuration
class AppConfig{
    @Bean
    fun hashProvider(): HashProvider = SHA256HashProvider()
    @Bean
    fun cryptoProvider(): CryptoProvider = RSACryptoProvider()
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

@Component
class ApplicationInitializer {
    @PostConstruct
    fun initialize() {
    }
}