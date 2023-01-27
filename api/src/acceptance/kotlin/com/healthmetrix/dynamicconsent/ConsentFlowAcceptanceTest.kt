package com.healthmetrix.dynamicconsent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.result.Ok
import com.healthmetrix.dynamicconsent.commons.json
import com.healthmetrix.dynamicconsent.consent.ConsentOption
import com.healthmetrix.dynamicconsent.signing.usecases.ConsentInfo
import com.healthmetrix.dynamicconsent.signing.verifyjwt.JwtInfo
import com.healthmetrix.dynamicconsent.signing.verifyjwt.TokenVerifier
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID

@SpringBootTest(
    classes = [DynamicConsentApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [],
)
@AutoConfigureMockMvc
@ActiveProfiles("local-repositories", "postgres")
class ConsentFlowAcceptanceTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var tokenVerifier: TokenVerifier

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val signatureDataURI =
        "data:image/png;base64,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uu73FUro1ehtz3JU0pLestZfHQGPYU/A34wQY0Z778+aWxvicMdazvBxgHbHRrvz31cfetQiQHky3qJ5ov6P4yFU226eWcAY/9ASwxR4p+V1bXH/0nO8XVjzcqGu3FnLQaaUa7QeDxSDSKsd7NXr1j7sjxk/d2j8nI8YvzTanpdVT+PJW173mp9wOxlz8uNzrS1pzacC1NuSKC5TrmC33iBJy2usXRG+wM9eUwl2z69a8jXrGre8PnMXbmfWFs8hH+BRNTZiqX5IXYkQk5xSI8p5CetkvMPqT9Ci5bW32fzNtQNcAK4B1ynXAi1LVAUnn4T+eUtd6YYxu9Q26Z1FjOkqy3u2v6W8sCHiTduGe1PWv2w+Yvkq+HuUSecnwC7HiSWZlV3gEcrktL+kHqt8dYI4LdbVtwfApQ51dpUZMU2BuxFjmhuxeuvp2/TnrgfZEgntyeE7Tt7LSLBv6/BZ7uDmjVG01H+L8PMzbfNF7weOY5U97LVa1WHiNTS/JR2NKE+kB9gnWpc5khHw+5Z6XZYxbodty3qIh5S9w8Rhe20x3G2JoU/3aVaNWP1kIs1vycausnzF+gFuJnRTVg8bTloK4MeUK759SCh7MctiuAc8i/vksUf7HF1fk1XVDuv9vW9TfljuUs51JRFilQF4CPiI5SfTDdx33/coP4EnK9qu9u4Kyit9zxD+atgIe6eHtuR1nfLCwMVZrMns6y7lTq27s7IzK+ns/69QvnFfA/61pI2xx2P1YYf1Nk6sl/1ZuUb5uzmLhpKnVgL8mfaT5QvCPSQipezl5ZXSx0/aHdZPsj7KJ5RLTIYqw93vb59yLnHeM3uY44s1VP57NCsJxx8edUnLv0tPZCwfAhboD7hMhvuh7ario6cbyx7r91hDlkO0x1evZLTvtT6lfBPqD7a+hDKRfIafN9C3lD3PbUlUloxyiFtQHm/spDWUm963XkZ7b+ol4AexAtsSCWVyKVh+WX+dcoC/LWL6LqGcIsgpPwwKwvZmQ9/eJC0yFv8oGdv9yd0H8wn1ajlX+e+92dfLs69J0OiGZ8TxfOc8mXX9cKiWIzTC6JWMft0TJ+JKQvPDIaW8Eq5JdxERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERFx4P/cv8rXKZf86AAAAABJRU5ErkJggg=="

    private val successRedirectUrl = "com.healthmetrix.researchapp://signing"

    private lateinit var bearerToken: String
    private lateinit var validAuthToken: String
    private lateinit var consentId: String

    @BeforeEach
    fun init() {
        bearerToken = UUID.randomUUID().toString()
        validAuthToken = "Bearer $bearerToken"
    }

    @ParameterizedTest
    @ValueSource(strings = ["smart4health-research-consent-en"])
    fun `Load consent flow, load form, sign, load pdf, withdraw`(consentIdParam: String) {
        consentId = consentIdParam
        every { tokenVerifier.verify(any(), any()) } returns Ok(JwtInfo(bearerToken))

        generateConsentFlow()
        val pdf = generateConsentPdf()
        val cacheDocumentResponse = cacheDocument(pdf)
        val documentId = cacheDocumentResponse.documentId
        val signatureToken = cacheDocumentResponse.token

        loadSignatureForm(documentId, signatureToken)
        signAndPersistDocument(documentId, signatureToken)
            .also {
                assertThat(it.successRedirectUrl).isEqualTo(successRedirectUrl)
            }

        getSignedPdf()
            .also {
                assertThat(it).isNotNull
                assertThat(it.size).isGreaterThan(50000) // 50KB
            }

        getAllConsentInfo()
            .also {
                assertThat(it.consented).hasSize(1)
                assertThat(it.consented[0].consentFlowId).isNotNull
                assertThat(it.consented[0].signedAt).isNotNull
                assertThat(it.consented[0].withdrawnAt).isNull()
                assertThat(it.consented[0].consentId).isEqualTo(consentId)
                assertThat(it.withdrawn).isEmpty()
            }

        withdrawConsent()

        getAllConsentInfo()
            .also {
                assertThat(it.consented).isEmpty()
                assertThat(it.withdrawn).hasSize(1)
                assertThat(it.withdrawn[0].withdrawnAt).isNotNull
            }

        // PDF should be there even if it is already withdrawn
        val signedPdf = getSignedPdf()
            .also {
                assertThat(it).isNotNull
                assertThat(it.size).isGreaterThan(50000) // 50KB
            }

        // Consent a second time for the same consentId that was just withdrawn for
        generateConsentFlow()
        val pdfTwo = generateConsentPdf()
        val cacheDocumentResponseTwo = cacheDocument(pdfTwo)
        val documentIdTwo = cacheDocumentResponseTwo.documentId
        val signatureTokenTwo = cacheDocumentResponse.token
        loadSignatureForm(documentIdTwo, signatureTokenTwo)
        signAndPersistDocument(documentIdTwo, signatureTokenTwo)

        // Status endpoint should return exactly one item since it's the same consentId
        getAllConsentInfo()
            .also {
                assertThat(it.consented).hasSize(1)
                assertThat(it.consented[0].withdrawnAt).isNull()
                assertThat(it.withdrawn).isEmpty()
            }

        // PDF should be the last signed one, not the first withdrawn
        getSignedPdf()
            .also {
                assertThat(it).isNotNull
                assertThat(it.size).isGreaterThan(50000) // 50KB
                assertThat(it).isNotEqualTo(signedPdf)
            }
    }

    private fun withdrawConsent() =
        mockMvc.delete("/api/v2/signatures?consentId=$consentId") {
            contentType = MediaType.APPLICATION_JSON
            headers {
                add("Authorization", validAuthToken)
            }
        }.andExpect { status { isOk() } }

    private fun generateConsentFlow() {
        mockMvc.get("/consents/$consentId") {
            param("successRedirectUrl", successRedirectUrl)
        }.andExpect {
            status { isOk() }
        }
    }

    private fun getAllConsentInfo() =
        mockMvc.get("/api/v2/signatures/status") {
            contentType = MediaType.APPLICATION_JSON
            headers {
                add("Authorization", validAuthToken)
            }
        }.andExpect {
            status { isOk() }
        }.andReturn().response.let {
            objectMapper.readValue<GetAllConsentsResponse>(it.contentAsByteArray)
        }

    private fun generateConsentPdf() = mockMvc.post("/api/v1/consents/$consentId/documents") {
        contentType = MediaType.APPLICATION_JSON
        content = json {
            "options" to consentOptions
        }.toString()
    }.andExpect {
        status { isCreated() }
    }.andReturn().response.contentAsByteArray

    private fun cacheDocument(pdf: ByteArray) = mockMvc.post("/api/v1/signatures") {
        contentType = MediaType.APPLICATION_PDF
        content = pdf
        headers {
            add("X-Hmx-Success-Redirect-Url", successRedirectUrl)
            add("X-Hmx-Consent-Id", consentId)
            add("Authorization", validAuthToken)
        }
    }.andExpect {
        status { isCreated() }
    }.andReturn().response.let {
        objectMapper.readValue<CacheDocumentResponse>(it.contentAsByteArray)
    }

    private fun loadSignatureForm(documentId: String, signatureToken: String) {
        mockMvc.get("/signatures/$documentId/sign") {
            param("token", signatureToken)
            param("Authorization", validAuthToken)
        }
    }

    private fun signAndPersistDocument(documentId: String, signatureToken: String) =
        mockMvc.put("/api/v1/signatures/$documentId/sign") {
            contentType = MediaType.APPLICATION_JSON
            headers {
                add("Authorization", validAuthToken)
            }
            content = json {
                "firstName" to "devBob"
                "lastName" to "Homedoggie"
                "token" to signatureToken
                "signature" to signatureDataURI
            }.toString()
        }.andExpect { status { isOk() } }.andReturn().response.let {
            objectMapper.readValue<SignDocumentResponse>(it.contentAsByteArray)
        }

    private fun getSignedPdf(): ByteArray =
        mockMvc.get("/api/v2/signatures?consentId=$consentId") {
            contentType = MediaType.APPLICATION_OCTET_STREAM
            headers {
                add("Authorization", validAuthToken)
            }
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsByteArray

    data class SignDocumentResponse(val successRedirectUrl: String)
    data class CacheDocumentResponse(val documentId: String, val token: String)
    data class GetAllConsentsResponse(val consented: List<ConsentInfo>, val withdrawn: List<ConsentInfo>)

    private val consentOptions = listOf(
        ConsentOption(optionId = 0, consented = true),
        ConsentOption(optionId = 1, consented = false),
        ConsentOption(optionId = 2, consented = true),
        ConsentOption(optionId = 3, consented = false),
        ConsentOption(optionId = 4, consented = true),
    )
}
