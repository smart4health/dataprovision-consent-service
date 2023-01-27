package com.healthmetrix.dynamicconsent.persistence.signing

import com.github.michaelbull.result.Result
import com.healthmetrix.dynamicconsent.commons.logger
import com.healthmetrix.dynamicconsent.persistence.signing.api.SignedConsent
import com.healthmetrix.dynamicconsent.persistence.signing.api.SignedConsentRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import com.github.michaelbull.result.runCatching as catch

@Component
@Profile("!rds & !postgres")
class InMemorySignedConsentRepository : SignedConsentRepository {

    private val consentSet = mutableSetOf<SignedConsent>()

    override fun recordConsent(consent: SignedConsent): Result<Unit, Throwable> = catch {
        logger.info("Recording consent: ${consent.consentFlowId}")
        consentSet.add(consent)
    }

    override fun findById(consentFlowId: String): Result<SignedConsent, Throwable> = catch {
        consentSet.single { it.consentFlowId == consentFlowId }
    }

    override fun findByDocumentId(documentId: String): Result<SignedConsent?, Throwable> = catch {
        consentSet.firstOrNull { it.documentId == documentId }
    }
}
