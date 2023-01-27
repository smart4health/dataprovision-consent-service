package com.healthmetrix.dynamicconsent.commons

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class ObjectMapperConfig {
    @Bean("jsonObjectMapper")
    @Primary
    fun jsonObjectMapper() = jacksonObjectMapper()
        .registerKotlinModule()

    @Bean("yamlObjectMapper")
    fun yamlObjectMapper() = JsonMapper.builder(YAMLFactory())
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()
        .registerKotlinModule()
}
