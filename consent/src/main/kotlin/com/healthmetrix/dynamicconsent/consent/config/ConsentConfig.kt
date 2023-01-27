package com.healthmetrix.dynamicconsent.consent.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.flatMap
import com.healthmetrix.dynamicconsent.commons.SecretKey
import com.healthmetrix.dynamicconsent.commons.Secrets
import com.healthmetrix.dynamicconsent.commons.decodeBase64String
import com.healthmetrix.dynamicconsent.commons.orThrow
import com.healthmetrix.dynamicconsent.commons.pdf.SigningMaterial
import com.healthmetrix.dynamicconsent.commons.pdf.decodePrivateKey
import com.healthmetrix.dynamicconsent.commons.pdf.decodeX509Certificate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val CONSENT_API_TAG = "Consent API"
const val DOCUMENT_API_TAG = "Document API"

@Configuration
class ConsentConfig {
    @Bean("consentSigningMaterial")
    fun provideConsentSigningMaterial(secrets: Secrets, objectMapper: ObjectMapper): SigningMaterial {
        val json = secrets.get(SecretKey.CONSENT_SIGNING_MATERIAL)
        val map = objectMapper.readValue<Map<String, String>>(json)

        val privateKey = (map["private-key"] ?: error("No private key found"))
            .decodeBase64String()
            .flatMap(String::decodePrivateKey)
            .orThrow()

        val publicCert = (map["public-cert"] ?: error("No public cert found"))
            .decodeBase64String()
            .flatMap(String::decodeX509Certificate)
            .orThrow()

        return SigningMaterial(privateKey, publicCert)
    }
}
