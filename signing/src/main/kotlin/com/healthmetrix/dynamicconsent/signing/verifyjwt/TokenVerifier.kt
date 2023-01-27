package com.healthmetrix.dynamicconsent.signing.verifyjwt

import com.auth0.jwt.interfaces.DecodedJWT
import com.github.michaelbull.result.Result

data class JwtInfo(
    val externalRefId: String,
    val metadata: Map<String, String>? = null,
)

interface TokenVerifier {
    val issuers: List<String>

    fun verify(encoded: String, decoded: DecodedJWT): Result<JwtInfo, Throwable>
}

class NoPatientException : Exception("Failed to find a consent flow id")
