package com.healthmetrix.dynamicconsent.persistence.signing

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.dynamicconsent.commons.SecretKey
import com.healthmetrix.dynamicconsent.commons.Secrets
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories
class DatabaseConfiguration {

    @Bean
    @Profile("postgres")
    fun provideCredentials(secrets: Secrets, objectMapper: ObjectMapper): Credentials =
        secrets.get(SecretKey.DB_CREDENTIALS).let(objectMapper::readValue)

    @ConfigurationProperties("spring.datasource")
    @Bean
    @Primary
    @Profile("postgres")
    fun provideDataSourceProperties() = DataSourceProperties()

    @Bean
    @Primary
    @Profile("postgres")
    fun provideDataSource(
        credentials: Credentials,
        dataSourceProperties: DataSourceProperties,
        config: SigningDatabaseConfig,
    ): DataSource {
        if (config.databaseName.isBlank()) {
            throw IllegalArgumentException("signing.database.database-name should not be blank")
        }

        return dataSourceProperties.initializeDataSourceBuilder().apply {
            StringBuilder("jdbc:postgresql://${config.endpoint}/${config.databaseName}").apply {
                append("?currentSchema=${config.schema ?: "public"}")
                config.additionalParams?.forEach { (key, value) ->
                    append("&$key=$value")
                }
            }.toString().let(this::url)

            username(credentials.username)
            password(credentials.password)
        }.build()
    }

    @ConstructorBinding
    @ConfigurationProperties(prefix = "signing.database")
    data class SigningDatabaseConfig(
        val endpoint: String,
        val databaseName: String,
        val schema: String?,
        val additionalParams: Map<String, String>?,
    )

    data class Credentials(
        val username: String,
        val password: String,
    )
}
