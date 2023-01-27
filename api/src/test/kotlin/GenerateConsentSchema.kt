package com.healthmetrix.dynamicconsent.consent.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.healthmetrix.dynamicconsent.commons.pdf.ConsentPdfConfig
import com.healthmetrix.dynamicconsent.consent.templating.ConsentTemplate
import com.healthmetrix.dynamicconsent.signing.ConsentSigningTemplate
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Usage see https://github.com/mbknor/mbknor-jackson-jsonSchema
 */
const val DIR = "./build/json-schema/"

class GenerateConsentSchema {
    private val objectMapper = jacksonObjectMapper().registerKotlinModule()
    private val jsonSchemaGenerator = JsonSchemaGenerator(objectMapper)

    @Test
    fun generateAllSchema() {
        val consentTemplate: String =
            objectMapper.writeValueAsString(jsonSchemaGenerator.generateJsonSchema(ConsentTemplate::class.java))
        val consentTemplateFile = File(DIR, "template.json")
        consentTemplateFile.parentFile.mkdirs()
        consentTemplateFile.writeText(consentTemplate)

        val consentConfig: String =
            objectMapper.writeValueAsString(jsonSchemaGenerator.generateJsonSchema(ConsentPdfConfig::class.java))
        val consentConfigFile = File(DIR, "config.json")
        consentConfigFile.parentFile.mkdirs()
        consentConfigFile.writeText(consentConfig)

        val signingTemplate: String =
            objectMapper.writeValueAsString(jsonSchemaGenerator.generateJsonSchema(ConsentSigningTemplate::class.java))
        val signingTemplateFile = File("$DIR/signing", "template.json")
        signingTemplateFile.parentFile.mkdirs()
        signingTemplateFile.writeText(signingTemplate)
    }
}
