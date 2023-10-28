package com.app.stocks_service

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
@ContextConfiguration(initializers = [CleanDatabaseTest.SpringInitializer::class])
abstract class CleanDatabaseTest : BaseTest() {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer(DockerImageName.parse("postgres:16.0")).apply {
            this.withDatabaseName("stocks").withUsername("postgres").withPassword("postgres")
        }
    }

    internal class SpringInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            val properties = TestPropertyValues.of(
                "spring.datasource.url=${postgresContainer.jdbcUrl}",
                "spring.datasource.username=${postgresContainer.username}",
                "spring.datasource.password=${postgresContainer.password}",
            )
            properties.applyTo(applicationContext.environment)

            postgresContainer.start()
        }
    }

    @BeforeEach
    fun clearTables() {
        jdbcTemplate.execute("TRUNCATE TABLE stock_prices CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE companies CASCADE")
    }
}