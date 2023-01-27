package com.healthmetrix.dynamicconsent.signing.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.dynamicconsent.signing.DocumentID
import com.healthmetrix.dynamicconsent.signing.SignatureToken
import com.healthmetrix.dynamicconsent.signing.usecases.CacheDocumentUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.DecryptSignatureTokenUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.EncryptSignatureTokenUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.GetLeastRecentlySignedPdfUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.SelectSigningTemplateUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.SignPdfUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.WithdrawSignatureError
import com.healthmetrix.dynamicconsent.signing.usecases.WithdrawSignatureUseCase
import com.healthmetrix.dynamicconsent.signing.verifyjwt.JwtInfo
import com.healthmetrix.dynamicconsent.signing.verifyjwt.JwtInfoExtractor
import com.healthmetrix.dynamicconsent.signing.views.SignDocumentView
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class SignatureControllerTest {
    private val cacheDocumentUseCase: CacheDocumentUseCase = mockk()
    private val jwtInfoExtractor: JwtInfoExtractor = mockk()
    private val signDocumentView: SignDocumentView = mockk()
    private val signPdfUseCase: SignPdfUseCase = mockk()
    private val getLeastRecentlySignedPdfUseCase: GetLeastRecentlySignedPdfUseCase = mockk()
    private val encryptSignatureTokenUseCase: EncryptSignatureTokenUseCase = mockk()
    private val decryptSignatureTokenUseCase: DecryptSignatureTokenUseCase = mockk()
    private val selectSigningTemplateUseCase: SelectSigningTemplateUseCase = mockk()
    private val withdrawSignatureUseCase: WithdrawSignatureUseCase = mockk()
    val objectMapper = ObjectMapper().registerKotlinModule()

    private val underTest = SignatureController(
        jwtInfoExtractor,
        cacheDocumentUseCase,
        signDocumentView,
        signPdfUseCase,
        getLeastRecentlySignedPdfUseCase,
        encryptSignatureTokenUseCase,
        decryptSignatureTokenUseCase,
        selectSigningTemplateUseCase,
        withdrawSignatureUseCase,
    )

    private val mockMvc = MockMvcBuilders.standaloneSetup(underTest).build()

    private val jwtInfo = JwtInfo("fakeConsentFlowId")
    private val document = "fake document".toByteArray()
    private val consentId = "fake-consent-id"

    @Nested
    inner class CreateSignatureEndpointTest {
        private val documentId = DocumentID.randomUUID()

        private fun sendRequest(block: MockHttpServletRequestDsl.() -> Unit? = {}): ResultActionsDsl {
            return mockMvc.post("/api/v1/signatures") {
                contentType = MediaType.APPLICATION_PDF
                content = document
                headers {
                    add("X-Hmx-Success-Redirect-Url", "lol.test")
                    add("X-Hmx-Cancel-Redirect-Url", "cancel.test")
                    add("X-Hmx-Consent-Id", "a real consent id!")
                    add("Authorization", "Bearer lol")
                    add("X-Hmx-Platform", SignaturePlatform.ANDROID.platform)
                }
                block()
            }
        }

        @Test
        fun `returns 201 when document is stored and signature token is created`() {
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every { cacheDocumentUseCase(any(), any(), any()) } returns Ok(documentId.toString())
            every { encryptSignatureTokenUseCase(any()) } returns Ok("fake token")

            sendRequest().andExpect {
                status { isCreated() }
            }
        }

        @Test
        fun `returns 500 when storing document fails`() {
            every { cacheDocumentUseCase(any(), any(), any()) } returns Err(Exception("error!"))
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            sendRequest().andExpect {
                status { isInternalServerError() }
            }
        }

        @Test
        fun `returns 500 when error creating Signature Token`() {
            every { cacheDocumentUseCase(any(), any(), any()) } returns Ok(documentId.toString())
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every { encryptSignatureTokenUseCase(any()) } returns Err(Exception("error!"))
            sendRequest().andExpect {
                status { isInternalServerError() }
            }
        }
    }

    @Nested
    inner class GetSignatureEndpointTest {
        @Test
        fun `receives 200 when document successfully found, v1`() {
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every { getLeastRecentlySignedPdfUseCase(ofType(String::class), ofType(DocumentID::class)) } returns Ok(document)

            mockMvc.get("/api/v1/signatures/${DocumentID.randomUUID()}") {
                contentType = MediaType.APPLICATION_OCTET_STREAM
                headers {
                    add("Authorization", "Bearer lol")
                }
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        fun `receives 200 when document successfully found, v2`() {
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every { getLeastRecentlySignedPdfUseCase(ofType(String::class), eq(consentId)) } returns Ok(document)

            mockMvc.get("/api/v2/signatures?consentId=$consentId") {
                contentType = MediaType.APPLICATION_OCTET_STREAM
                headers {
                    add("Authorization", "Bearer lol")
                }
            }.andExpect {
                status { isOk() }
            }
        }
    }

    @Nested
    inner class SignDocumentEndpointTest {
        private val documentId = DocumentID.randomUUID()
        private val requestBody = mapOf(
            "firstName" to "devbob",
            "lastName" to "mcpoopypants",
            "token" to "fake token",
            "signature" to "fake signature",
        )

        private fun sendRequest(block: MockHttpServletRequestDsl.() -> Unit): ResultActionsDsl {
            return mockMvc.put("/api/v1/signatures/$documentId/sign") {
                contentType = MediaType.APPLICATION_JSON
                headers {
                    add("Authorization", "Bearer lol")
                }
                block()
            }
        }

        @Test
        fun `it returns 200 when successfully updating and storing pdf`() {
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every { signPdfUseCase(any(), any(), any(), any(), any(), any()) } returns Ok(Unit)
            every { decryptSignatureTokenUseCase(any()) } returns Ok(
                SignatureToken(
                    "fake success redirect url",
                    "fake auth token",
                    consentId = consentId,
                    platform = null,
                ),
            )
            sendRequest {
                content = objectMapper.writeValueAsBytes(requestBody)
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        fun `it returns 500 if signing pdf errors`() {
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every { signPdfUseCase(any(), any(), any(), any(), any(), any()) } returns Err(Exception("error!"))
            every { decryptSignatureTokenUseCase(any()) } returns Ok(
                SignatureToken(
                    "fake success redirect url",
                    "fake auth token",
                    consentId = consentId,
                    platform = null,
                ),
            )

            sendRequest {
                content = objectMapper.writeValueAsBytes(requestBody)
            }.andExpect {
                status { isInternalServerError() }
            }
        }

        @Test
        fun `it returns 500 if decoding jwt errors`() {
            every { signPdfUseCase(any(), any(), any(), any(), any(), any()) } returns Ok(Unit)
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every { decryptSignatureTokenUseCase(any()) } returns Err(Exception("error!"))
            sendRequest {
                content = objectMapper.writeValueAsBytes(requestBody)
            }.andExpect {
                status { isInternalServerError() }
            }
        }

        @Test
        fun `it expects authorization header`() {
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every { signPdfUseCase(any(), any(), any(), any(), any(), any()) } returns Ok(Unit)
            every { decryptSignatureTokenUseCase(any()) } returns Ok(
                SignatureToken(
                    "fake success redirect url",
                    "fake auth token",
                    consentId = consentId,
                    platform = null,
                ),
            )
            sendRequest {
                headers {
                    remove("Authorization")
                }
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    inner class WithdrawSignatureEndpointTest {
        private val documentId = "document-id"

        @Test
        fun `it returns 200 when successfully withdrawing consent`() {
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)

            every { withdrawSignatureUseCase.invoke(documentId) } returns Ok(Unit)
            mockMvc.post("/api/v1/signatures/$documentId/withdraw") {
                contentType = MediaType.APPLICATION_JSON
                headers {
                    add("Authorization", "Bearer lol")
                }
            }.andExpect { status { isOk() } }
        }

        @Test
        fun `it returns 404 if signature not found`() {
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every {
                withdrawSignatureUseCase.invoke(documentId)
            } returns Err(WithdrawSignatureError.SignatureNotFound)
            mockMvc.post("/api/v1/signatures/$documentId/withdraw") {
                contentType = MediaType.APPLICATION_JSON
                headers {
                    add("Authorization", "Bearer lol")
                }
            }.andExpect { status { isNotFound() } }
        }

        @Test
        fun `it returns 409 if signature already withdrawn`() {
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every {
                withdrawSignatureUseCase.invoke(documentId)
            } returns Err(WithdrawSignatureError.SignatureAlreadyWithdrawn)
            mockMvc.post("/api/v1/signatures/$documentId/withdraw") {
                contentType = MediaType.APPLICATION_JSON
                headers {
                    add("Authorization", "Bearer lol")
                }
            }.andExpect { status { isConflict() } }
        }
    }

    @Nested
    inner class WithdrawSignatureV2EndpointTest {

        @Test
        fun `it returns 200 when successfully withdrawing consent`() {
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every {
                withdrawSignatureUseCase.invoke(consentId = consentId, externalRefId = jwtInfo.externalRefId)
            } returns Ok(Unit)
            mockMvc.delete("/api/v2/signatures?consentId=$consentId") {
                contentType = MediaType.APPLICATION_JSON
                headers {
                    add("Authorization", "Bearer lol")
                }
            }.andExpect { status { isOk() } }
        }

        @Test
        fun `it returns 400 if update failed`() {
            every { jwtInfoExtractor(any()) } returns Ok(jwtInfo)
            every {
                withdrawSignatureUseCase.invoke(consentId = consentId, externalRefId = jwtInfo.externalRefId)
            } returns Err(WithdrawSignatureError.UpdateWithdrawnAtFailed)
            mockMvc.delete("/api/v2/signatures?consentId=$consentId") {
                contentType = MediaType.APPLICATION_JSON
                headers {
                    add("Authorization", "Bearer lol")
                }
            }.andExpect { status { isBadRequest() } }
        }
    }
}
