package com.healthmetrix.dynamicconsent.consent.controllers

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.dynamicconsent.consent.fakeConsentTemplate
import com.healthmetrix.dynamicconsent.consent.usecases.LoadTemplateModelUseCase
import com.healthmetrix.dynamicconsent.consent.usecases.SelectTemplateUseCase
import com.healthmetrix.dynamicconsent.consent.usecases.TemplateModel
import com.healthmetrix.dynamicconsent.consent.views.ConsentView
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ConsentControllerTest {
    private val loadTemplateModelUseCase: LoadTemplateModelUseCase = mockk()

    private val consentView: ConsentView = mockk()

    private val selectTemplateUseCase: SelectTemplateUseCase = mockk()

    private val consentController = ConsentController(
        loadTemplateModelUseCase,
        consentView,
        selectTemplateUseCase,
    )

    private val successRedirectUrl = "https://test.com"
    private val cancelRedirectUrl = "https://redirect.test"

    private val mvc = MockMvcBuilders.standaloneSetup(consentController).build()

    @Nested
    inner class GenerateConsentFlowTest {
        @Test
        fun `returns 200 on success`() {
            every { loadTemplateModelUseCase(any(), any(), any(), any()) } returns TemplateModel(listOf(), null)
            every { consentView.build(any(), any(), any()) } returns StringBuilder()
            every { selectTemplateUseCase(any()) } returns Ok(fakeConsentTemplate)
            val response = mvc.get("/consents/fakeConsent") {
                param("successRedirectUrl", successRedirectUrl)
                param("cancelRedirectUrl", cancelRedirectUrl)
                param("platform", "android")
            }.andReturn().response

            assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        }

        @Test
        fun `throws an exception if it errors`() {
            every { selectTemplateUseCase(any()) } returns Err(Error("error!"))
            assertThrows<Throwable> {
                mvc.get("/consents/fakeConsent") {
                    param("successRedirectUrl", successRedirectUrl)
                    param("cancelRedirectUrl", cancelRedirectUrl)
                    param("platform", "android")
                }
            }
        }
    }
}
