package com.healthmetrix.dynamicconsent.persistence.signing

import com.github.michaelbull.result.Result
import com.healthmetrix.dynamicconsent.commons.logger
import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsent
import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsentRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import com.github.michaelbull.result.runCatching as catch

@Component
@Profile("!rds & !postgres")
class InMemoryCachedConsentRepository : CachedConsentRepository {
    private val consentMapOf = mutableMapOf<String, CachedConsent>()

    override fun save(consent: CachedConsent): Result<Unit, Throwable> = catch {
        logger.info("Saving consent: ${consent.consentFlowId}")
        consentMapOf[consent.consentFlowId] = consent
    }

    override fun findById(consentFlowId: String): CachedConsent? = consentMapOf[consentFlowId]

    override fun findByDocumentId(documentId: String): Result<CachedConsent?, Throwable> = catch {
        consentMapOf.values.firstOrNull { it.documentId == documentId }
    }
}
