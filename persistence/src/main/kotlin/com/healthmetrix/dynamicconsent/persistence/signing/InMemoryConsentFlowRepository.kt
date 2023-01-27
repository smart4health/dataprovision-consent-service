package com.healthmetrix.dynamicconsent.persistence.signing

import com.github.michaelbull.result.Result
import com.healthmetrix.dynamicconsent.commons.logger
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlow
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlowRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import com.github.michaelbull.result.runCatching as catch

@Component
@Profile("!rds & !postgres")
class InMemoryConsentFlowRepository : ConsentFlowRepository {
    private val consentMapOf = mutableMapOf<String, ConsentFlow>()

    override fun recordConsentFlow(consent: ConsentFlow): Result<Unit, Throwable> = catch {
        logger.info("Recording consent: ${consent.consentFlowId}")
        consentMapOf[consent.consentFlowId] = consent
    }

    override fun findById(consentFlowId: String): ConsentFlow? = consentMapOf[consentFlowId]

    override fun findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(
        externalRefId: String,
        consentId: String,
    ): Result<ConsentFlow?, Throwable> = catch {
        consentMapOf.values.firstOrNull { it.externalRefId == externalRefId && it.consentId == consentId }
    }

    override fun findByExternalRefId(externalRefId: String): List<ConsentFlow> =
        consentMapOf.values.filter { it.externalRefId == externalRefId }

    override fun findByExternalRefIdAndConsentId(
        externalRefId: String,
        consentId: String,
    ): List<ConsentFlow> =
        consentMapOf.values.filter { it.externalRefId == externalRefId && it.consentId == consentId }
}
