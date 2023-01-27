package com.healthmetrix.dynamicconsent.signing.usecases

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toErrorIf
import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsent
import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsentRepository
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlow
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlowRepository
import com.healthmetrix.dynamicconsent.signing.ConsentFlowID
import com.healthmetrix.dynamicconsent.signing.DocumentID
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date

@Service
class CacheDocumentUseCase(
    private val cachedConsentRepository: CachedConsentRepository,
    private val consentFlowRepository: ConsentFlowRepository,
) {
    operator fun invoke(externalRefId: String, document: ByteArray, consentId: String): Result<String, Throwable> =
        binding {
            val existingFlow =
                consentFlowRepository.findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(externalRefId, consentId)
                    .toErrorIf(
                        predicate = { it?.signedAt != null },
                        transform = { CacheDocumentError.AlreadyConsentedException },
                    )
                    .bind()

            val consentFlowId = existingFlow?.consentFlowId ?: ConsentFlowID.randomUUID().toString()
            val documentId = DocumentID.randomUUID().toString()
            consentFlowRepository.recordConsentFlow(
                ConsentFlow(
                    consentFlowId = consentFlowId,
                    externalRefId = externalRefId,
                    consentId = consentId,
                ),
            ).bind()

            cachedConsentRepository.save(
                CachedConsent(
                    documentId = documentId,
                    consentFlowId = consentFlowId,
                    document = document,
                    consentId = consentId,
                    createdAt = Date.from(Instant.now()),
                ),
            ).bind()

            documentId
        }
}

sealed class CacheDocumentError : Exception() {
    object AlreadyConsentedException : CacheDocumentError()
}
