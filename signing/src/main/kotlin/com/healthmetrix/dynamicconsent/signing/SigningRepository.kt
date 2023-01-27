package com.healthmetrix.dynamicconsent.signing

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.healthmetrix.dynamicconsent.commons.asLocalStaticResourcePath
import com.healthmetrix.dynamicconsent.commons.consent
import com.healthmetrix.dynamicconsent.commons.consentSource
import com.healthmetrix.dynamicconsent.commons.fetchYaml
import com.healthmetrix.dynamicconsent.commons.joinPaths
import com.healthmetrix.dynamicconsent.commons.pdf.ConsentPdfConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import com.github.michaelbull.result.runCatching as catch

interface SigningRepository {
    fun fetchConsentConfig(consentId: String): Result<ConsentPdfConfig, Throwable>
    fun fetchTemplate(consentId: String): Result<ConsentSigningTemplate, Throwable>
    fun staticPath(consentId: String, path: String): String
}

@Service
@Profile("remote-signing")
class RemoteSigningRepository(
    private val signingRepositoryConfig: SigningRepositoryConfig,
    webClientBuilder: WebClient.Builder,
    @Qualifier("yamlObjectMapper")
    private val objectMapper: ObjectMapper,
) : SigningRepository {
    private val webClient = webClientBuilder.build()

    override fun fetchConsentConfig(consentId: String): Result<ConsentPdfConfig, Throwable> =
        "${consentId.asBaseUrl()}/config.yaml"
            .let(webClient::fetchYaml)
            .flatMap { objectMapper.catch { readValue(it) } }

    override fun fetchTemplate(consentId: String): Result<ConsentSigningTemplate, Throwable> =
        staticPath(consentId, "template.yaml")
            .let(webClient::fetchYaml)
            .flatMap { objectMapper.catch { readValue(it) } }

    override fun staticPath(consentId: String, path: String): String {
        return consentId.asBaseUrl().joinPaths("signing", path)
    }

    private fun String.asBaseUrl(): String {
        val consentSource = signingRepositoryConfig.sources[this.consentSource()]
        return "$consentSource/${this.consent()}"
    }
}

@Service
@Profile("!remote-signing")
class LocalSigningRepository(
    @Value("classpath:static/consents/smart4health-research-consent-en/config.yaml")
    private val config: Resource,
    @Value("classpath:static/signings/smart4health-research-consent-en/template.yaml")
    private val template: Resource,
    @Qualifier("yamlObjectMapper")
    private val objectMapper: ObjectMapper,
) : SigningRepository {
    override fun fetchConsentConfig(consentId: String): Result<ConsentPdfConfig, Throwable> = catch {
        objectMapper.readValue(config.inputStream)
    }

    override fun fetchTemplate(consentId: String): Result<ConsentSigningTemplate, Throwable> = catch {
        objectMapper.readValue(template.inputStream)
    }

    /**
     * /test/test.svg => /consent-assets/signings/{consentId}/test/test.svg
     */
    override fun staticPath(consentId: String, path: String): String = "signings"
        .joinPaths(consentId, path)
        .asLocalStaticResourcePath()
}

@ConstructorBinding
@Profile("remote-signing")
@ConfigurationProperties(prefix = "remote-signing")
data class SigningRepositoryConfig(val sources: Map<String, String>)
