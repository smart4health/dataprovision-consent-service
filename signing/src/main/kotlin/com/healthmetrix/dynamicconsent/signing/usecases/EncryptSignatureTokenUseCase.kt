package com.healthmetrix.dynamicconsent.signing.usecases

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.runCatching
import com.healthmetrix.dynamicconsent.signing.AesKey
import com.healthmetrix.dynamicconsent.signing.SignatureToken
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class EncryptSignatureTokenUseCase(
    @Qualifier("signatureTokenKey")
    private val aesKey: AesKey,
    private val objectMapper: ObjectMapper,
) {
    operator fun invoke(signatureToken: SignatureToken): Result<String, Throwable> = binding {
        val jsonBytes = objectMapper.runCatching {
            writeValueAsBytes(signatureToken)
        }.bind()

        val encrypted = aesKey.encrypt(jsonBytes)

        Base64.getEncoder().encodeToString(encrypted)
    }
}
