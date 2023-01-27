package com.healthmetrix.dynamicconsent.config

import com.healthmetrix.dynamicconsent.consent.config.CONSENT_API_TAG
import com.healthmetrix.dynamicconsent.consent.config.DOCUMENT_API_TAG
import com.healthmetrix.dynamicconsent.signing.config.SIGNATURE_API_TAG
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val USER_AUTHENTICATION_SCHEME = "UserToken"

@Configuration
class DocumentationConfig(
    private val documentationInfo: DocumentationInfo,
) {
    @Bean
    fun api(): OpenAPI {
        return OpenAPI()
            .info(documentationInfo.toApiInfo())
            .addTagsItem(consentApiTag)
            .addTagsItem(signingApiTag)
            .addTagsItem(documentApiTag)
            .servers(documentationInfo.toServers())
            .components(
                Components()
                    .addSecuritySchemes(USER_AUTHENTICATION_SCHEME, bearerSecurityScheme),
            )
    }

    private val bearerSecurityScheme = SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .description("validated by JWKS")
    private val signingApiTag = Tag()
        .name(SIGNATURE_API_TAG)
        .description("API to support signing and generating PDFs")
    private val consentApiTag = Tag()
        .name(CONSENT_API_TAG)
        .description("API to load a dynamic consent flow and register the options a user consents to and rejects")
    private val documentApiTag = Tag()
        .name(DOCUMENT_API_TAG)
        .description("API to read and generate PDFs based on user-selected options from a consent template")
}

@ConfigurationProperties(prefix = "documentation-info")
@ConstructorBinding
data class DocumentationInfo(
    val title: String,
    val description: String,
    val contact: ContactConfig,
    val servers: List<ServerConfig>,
) {
    data class ContactConfig(
        val name: String,
        val url: String,
        val email: String,
    )

    data class ServerConfig(
        val url: String,
        val description: String,
    )

    fun toServers(): List<Server> {
        return servers.map { server ->
            Server()
                .url(server.url)
                .description(server.description)
        }
    }

    fun toApiInfo(): Info {
        return Info()
            .title(title)
            .description(description)
            .contact(
                Contact()
                    .url(contact.url)
                    .name(contact.name)
                    .email(contact.email),
            )
    }
}
