package com.healthmetrix.dynamicconsent.signing.usecases

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.healthmetrix.dynamicconsent.commons.jsonString
import com.healthmetrix.dynamicconsent.signing.AesKey
import com.healthmetrix.dynamicconsent.signing.SignatureToken
import com.healthmetrix.dynamicconsent.signing.controllers.SignaturePlatform
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SignatureTokenUseCaseTest {

    private val exampleSuccessRedirectUrl = "hello"
    private val exampleAuthToken = "world"

    private val mockAesKey = mockk<AesKey> {
        every { encrypt(any()) } returns ByteArray(16)

        every { decrypt(any()) } returns Ok(
            jsonString {
                "successRedirectUrl" to exampleSuccessRedirectUrl
                "authToken" to exampleAuthToken
                "consentId" to "consent id"
                "platform" to SignaturePlatform.ANDROID.platform
            }.toByteArray(),
        )
    }

    private val objectMapper = ObjectMapper().registerKotlinModule()

    private val encryptUnderTest = EncryptSignatureTokenUseCase(mockAesKey, objectMapper)
    private val decryptUnderTest = DecryptSignatureTokenUseCase(mockAesKey, objectMapper)

    @Test
    fun `encryption round trip results in the correct token`() {
        val original = SignatureToken(
            exampleSuccessRedirectUrl,
            exampleAuthToken,
            consentId = "consent id",
            platform = SignaturePlatform.ANDROID.platform,
        )
        val encrypted = encryptUnderTest(original).unwrap()
        val res = decryptUnderTest(encrypted).unwrap()

        assertThat(res).isEqualTo(original)
    }
}
