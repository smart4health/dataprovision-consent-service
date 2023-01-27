package com.healthmetrix.dynamicconsent.consent.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.dynamicconsent.consent.ConsentOption
import com.healthmetrix.dynamicconsent.consent.usecases.GeneratePdfUseCase
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class DocumentControllerTest {
    private val generatePdfUseCase: GeneratePdfUseCase = mockk()

    val objectMapper = ObjectMapper().registerKotlinModule()
    private val documentController = DocumentController(generatePdfUseCase)
    private val mvc = MockMvcBuilders.standaloneSetup(documentController).build()

    @Nested
    inner class GenerateDocumentEndpointTest {
        @Test
        fun `returns 201 on successful document generation`() {
            every { generatePdfUseCase(any(), any()) } returns Ok(ByteArray(0))
            mvc.post("/api/v1/consents/fakeConsent/documents") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_OCTET_STREAM
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "options" to listOf<ConsentOption>(),
                    ),
                )
            }.andExpect {
                status { isCreated() }
            }
        }

        @Test
        fun `returns 400 on invalid options`() {
            every { generatePdfUseCase(any(), any()) } returns Err(Throwable("Bad Doc"))
            mvc.post("/api/v1/consents/fakeConsent/documents") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsBytes(
                    mapOf(
                        "options" to listOf<ConsentOption>(),
                    ),
                )
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }
}
