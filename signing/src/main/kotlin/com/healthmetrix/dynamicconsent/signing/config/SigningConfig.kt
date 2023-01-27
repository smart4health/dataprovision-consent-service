package com.healthmetrix.dynamicconsent.signing.config

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
import com.healthmetrix.dynamicconsent.signing.AesKey
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

const val SIGNATURE_API_TAG = "Signature API"

@Configuration
class SigningConfig {

    @Bean("signatureTokenKey")
    fun provideSignatureTokenKey(secrets: Secrets): AesKey {
        val key = secrets.get(SecretKey.SIGNING_ENCRYPTION_KEY)

        val decoded = Base64.getDecoder().decode(key)

        if (decoded.size != 16) {
            throw IllegalStateException("signature token encryption key is not of length 16")
        }

        return AesKey(SecretKeySpec(decoded, "AES"))
    }

    @Bean("signingSigningMaterial")
    fun provideSigningSigningMaterial(secrets: Secrets, objectMapper: ObjectMapper): SigningMaterial {
        val json = secrets.get(SecretKey.SIGNING_SIGNING_MATERIAL)
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

    @Bean
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
