package com.healthmetrix.dynamicconsent.consent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.healthmetrix.dynamicconsent.commons.asLocalStaticResourcePath
import com.healthmetrix.dynamicconsent.commons.consent
import com.healthmetrix.dynamicconsent.commons.consentSource
import com.healthmetrix.dynamicconsent.commons.fetchPdf
import com.healthmetrix.dynamicconsent.commons.fetchYaml
import com.healthmetrix.dynamicconsent.commons.joinPaths
import com.healthmetrix.dynamicconsent.commons.pdf.ConsentPdfConfig
import com.healthmetrix.dynamicconsent.consent.templating.ConsentTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import com.github.michaelbull.result.runCatching as catch

private const val MAX_PDF_SIZE_BYTES = 5_000_000

interface ConsentRepository {
    fun fetchPdf(consentId: String): Result<ByteArray, Throwable>

    fun fetchTemplate(consentId: String): Result<ConsentTemplate, Throwable>

    fun fetchConfig(consentId: String): Result<ConsentPdfConfig, Throwable>

    fun staticPath(consentId: String, path: String): String
}

@Service
@Profile("remote-consent")
class RemoteConsentRepository(
    private val consentRepositoryConfig: ConsentRepositoryConfig,
    webClientBuilder: WebClient.Builder,
    @Qualifier("yamlObjectMapper")
    private val objectMapper: ObjectMapper,
) : ConsentRepository {
    private val webClient = webClientBuilder
        .exchangeStrategies(
            ExchangeStrategies
                .builder()
                .codecs { it.defaultCodecs().maxInMemorySize(MAX_PDF_SIZE_BYTES) }
                .build(),
        )
        .build()

    override fun fetchPdf(consentId: String): Result<ByteArray, Throwable> =
        staticPath(consentId, "consent.pdf").let(webClient::fetchPdf)

    override fun fetchTemplate(consentId: String): Result<ConsentTemplate, Throwable> =
        staticPath(consentId, "template.yaml")
            .let { webClient.fetchYaml(it) }
            .flatMap { objectMapper.catch { readValue(it) } }

    override fun fetchConfig(consentId: String): Result<ConsentPdfConfig, Throwable> =
        staticPath(consentId, "config.yaml")
            .let(webClient::fetchYaml)
            .flatMap { objectMapper.catch { readValue(it) } }

    override fun staticPath(consentId: String, path: String): String = consentId.asBaseUrl().joinPaths(path)

    private fun String.asBaseUrl(): String {
        val consentSource = consentRepositoryConfig.sources[this.consentSource()]
        return "$consentSource/${this.consent()}"
    }
}

@ConstructorBinding
@Profile("remote-consent")
@ConfigurationProperties(prefix = "remote-consent")
data class ConsentRepositoryConfig(val sources: Map<String, String>)

@Service
@Profile("!remote-consent")
class LocalConsentRepository(
    @Value("classpath:static/consents/smart4health-research-consent-en/consent.pdf")
    private val pdf: Resource,
    @Value("classpath:static/consents/smart4health-research-consent-en/config.yaml")
    private val config: Resource,
    @Value("classpath:static/consents/smart4health-research-consent-en/template.yaml")
    private val template: Resource,
    @Qualifier("yamlObjectMapper")
    private val objectMapper: ObjectMapper,
) : ConsentRepository {
    override fun fetchPdf(consentId: String): Result<ByteArray, Throwable> = catch {
        pdf.inputStream.readBytes()
    }

    override fun fetchTemplate(consentId: String): Result<ConsentTemplate, Throwable> = catch {
        objectMapper.readValue(template.inputStream)
    }

    override fun fetchConfig(consentId: String): Result<ConsentPdfConfig, Throwable> = catch {
        objectMapper.readValue(config.inputStream)
    }

    override fun staticPath(consentId: String, path: String): String =
        "/consents/$consentId/$path".asLocalStaticResourcePath()
}
