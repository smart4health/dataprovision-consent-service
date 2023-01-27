package com.healthmetrix.dynamicconsent.signing.verifyjwt

import com.auth0.jwt.JWT
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import com.github.michaelbull.result.runCatching as catch

interface JwtInfoExtractor {
    operator fun invoke(token: String): Result<JwtInfo, JwtInfoExtractorException>
}

@Component
@Profile("verify-jwt")
class VerifyingJwtInfoExtractor(
    private val verifiers: List<TokenVerifier>,
) : JwtInfoExtractor {
    @Suppress("ThrowableNotThrown")
    override operator fun invoke(token: String): Result<JwtInfo, JwtInfoExtractorException> =
        binding<JwtInfo, Throwable> {
            val decoded = catch {
                JWT.decode(token)
            }.bind()

            val verifier = verifiers
                .singleOrNull { it.issuers.contains(decoded.issuer) }
                .toResultOr { VerifierNotFoundException(decoded.issuer) }
                .bind()

            verifier.verify(token, decoded).bind()
        }.mapError(::JwtInfoExtractorException)

    class VerifierNotFoundException(issuer: String) : Exception("No verifier found for issuer $issuer")
}

@Component
@Profile("!verify-jwt")
class NonVerifyingJwtInfoExtractor : JwtInfoExtractor {
    override fun invoke(token: String): Result<JwtInfo, JwtInfoExtractorException> {
        return JwtInfo(externalRefId = token).let(::Ok)
    }
}

class JwtInfoExtractorException(ex: Throwable) : Exception(ex)
