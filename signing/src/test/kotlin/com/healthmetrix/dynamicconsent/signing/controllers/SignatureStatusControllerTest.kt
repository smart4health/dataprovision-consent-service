package com.healthmetrix.dynamicconsent.signing.controllers

import com.github.michaelbull.result.Ok
import com.healthmetrix.dynamicconsent.signing.usecases.ConsentInfo
import com.healthmetrix.dynamicconsent.signing.usecases.GetConsentInfoPerUserUseCase
import com.healthmetrix.dynamicconsent.signing.verifyjwt.JwtInfo
import com.healthmetrix.dynamicconsent.signing.verifyjwt.JwtInfoExtractor
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.Date

internal class SignatureStatusControllerTest {
    private val jwtInfoExtractor: JwtInfoExtractor = mockk()
    private val getConsentInfoPerUserUseCase: GetConsentInfoPerUserUseCase = mockk()
    private val underTest = SignatureStatusController(
        jwtInfoExtractor,
        getConsentInfoPerUserUseCase,
    )

    private val mockMvc = MockMvcBuilders.standaloneSetup(underTest).build()

    private val jwtInfo = JwtInfo("fakeConsentFlowId")

    @Test
    fun `it returns 200 when successfully fetching the status info`() {
        every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
        every { getConsentInfoPerUserUseCase(jwtInfo.externalRefId) } returns Pair(
            listOf(ConsentInfo("id", "cId", Date.from(Instant.now()), null)),
            emptyList(),
        )
        mockMvc.get("/api/v2/signatures/status") {
            contentType = MediaType.APPLICATION_JSON
            headers {
                add("Authorization", "Bearer lol")
            }
        }.andExpect {
            status { isOk() }
        }
    }
}
