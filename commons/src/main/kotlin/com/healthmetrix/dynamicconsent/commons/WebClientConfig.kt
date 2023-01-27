package com.healthmetrix.dynamicconsent.commons

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Configuration
class WebClientConfig {
    @Bean
    fun provideWebClientBuilder(): WebClient.Builder = WebClient.builder()
}

fun WebClient.fetchPdf(url: String): Result<ByteArray, Throwable> =
    this.get()
        .uri(url)
        .accept(MediaType.APPLICATION_PDF)
        .exchangeToMono { response -> response.bodyToMono<ByteArray>() }
        .block()
        .toResultOr { Error("The response body was null when it was expected to be ByteArray") }

fun WebClient.fetchYaml(url: String): Result<String, Throwable> =
    this.get()
        .uri(url)
        .accept(MediaType("application", "x-yaml"))
        .exchangeToMono { response -> response.bodyToMono<String>() }
        .block()
        .toResultOr { Error("The response body was null when it was expected to be String") }
