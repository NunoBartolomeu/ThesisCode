package com.ledger.app.configurations.database

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DriverManagerDataSource
import javax.sql.DataSource

@Configuration
class DatabaseConfig {
    @Value("\${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/ledgerdb}")
    private lateinit var url: String

    @Value("\${SPRING_DATASOURCE_USERNAME:postgres}")
    private lateinit var username: String

    @Value("\${SPRING_DATASOURCE_PASSWORD:.}")
    private lateinit var password: String

    @Bean
    fun dataSource(): DataSource {
        val dataSource = DriverManagerDataSource()
        dataSource.setDriverClassName("org.postgresql.Driver")
        dataSource.url = url
        dataSource.username = username
        dataSource.password = password
        return dataSource
    }
}