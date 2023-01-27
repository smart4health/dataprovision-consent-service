package com.healthmetrix.dynamicconsent.signing.usecases

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.runCatching
import com.healthmetrix.dynamicconsent.signing.AesKey
import com.healthmetrix.dynamicconsent.signing.SignatureToken
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class DecryptSignatureTokenUseCase(
    @Qualifier("signatureTokenKey")
    private val aesKey: AesKey,
    private val objectMapper: ObjectMapper,
) {
    operator fun invoke(token: String): Result<SignatureToken, Throwable> = binding {
        val encrypted = Base64.getDecoder().runCatching {
            decode(token)
        }.bind()

        val decrypted = aesKey.decrypt(encrypted).bind()

        objectMapper.runCatching {
            readValue<SignatureToken>(decrypted)
        }.bind()
    }
}
