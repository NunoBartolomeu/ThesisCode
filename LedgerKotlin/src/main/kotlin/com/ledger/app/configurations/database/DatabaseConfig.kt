package com.ledger.app.configurations.database

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DatabaseConfig {
    @Bean
    fun dataSource(): DataSource {
        val dataSource = org.springframework.jdbc.datasource.DriverManagerDataSource()
        dataSource.setDriverClassName("org.postgresql.Driver")
        dataSource.url = "jdbc:postgresql://localhost:5432/ledgerdb"
        dataSource.username = "postgres"
        dataSource.password = "."
        return dataSource
    }
}