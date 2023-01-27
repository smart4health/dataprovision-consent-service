package com.healthmetrix.dynamicconsent.persistence.signing.api

import com.github.michaelbull.result.Result
import java.util.Date

interface ConsentFlowRepository {
    fun recordConsentFlow(consent: ConsentFlow): Result<Unit, Throwable>

    fun findById(consentFlowId: String): ConsentFlow?

    fun findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(
        externalRefId: String,
        consentId: String,
    ): Result<ConsentFlow?, Throwable>

    fun findByExternalRefIdAndConsentId(
        externalRefId: String,
        consentId: String,
    ): List<ConsentFlow>

    fun findByExternalRefId(externalRefId: String): List<ConsentFlow>
}

data class ConsentFlow(
    val consentFlowId: String,
    val externalRefId: String,
    val consentId: String,
    val signedAt: Date? = null,
    val withdrawnAt: Date? = null,
)
