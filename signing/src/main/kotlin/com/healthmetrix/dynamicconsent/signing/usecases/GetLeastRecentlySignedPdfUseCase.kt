package com.healthmetrix.dynamicconsent.signing.usecases

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlowRepository
import com.healthmetrix.dynamicconsent.persistence.signing.api.SignedConsentRepository
import com.healthmetrix.dynamicconsent.signing.DocumentID
import org.springframework.stereotype.Service

@Service
class GetLeastRecentlySignedPdfUseCase(
    private val signedConsentRepository: SignedConsentRepository,
    private val consentFlowRepository: ConsentFlowRepository,
) {

    /**
     * Always returns the last item for a consentId the user has interacted with. Two options:
     * 1. the valid not-withdrawn one (nullsFirst). By database constraint there can only be one of this.
     * 2. the least recently withdrawn one (descending withdrawnAt)
     */
    operator fun invoke(externalRefId: String, consentId: String): Result<ByteArray, Throwable> = binding {
        val consentFlow =
            consentFlowRepository.findByExternalRefIdAndConsentId(externalRefId, consentId)
                .sortedWith(compareByDescending { it.signedAt })
                .firstOrNull()
                .toResultOr { GetLeastRecentlySignedPdfError.ConsentNotFound }
                .bind()

        signedConsentRepository.findById(consentFlow.consentFlowId)
            .mapError { GetLeastRecentlySignedPdfError.SignedConsentNotFound }
            .bind()
            .document
            .toResultOr { GetLeastRecentlySignedPdfError.EmptyDocument }
            .bind()
    }

    @Deprecated(
        "Use other invoke not using documentId, but consentId + externalRefId",
        replaceWith = ReplaceWith("GetSignedPdfUseCase.invoke(java.lang.String, java.lang.String)"),
    )
    operator fun invoke(externalRefId: String, documentId: DocumentID): Result<ByteArray, Throwable> =
        signedConsentRepository.findByDocumentId(documentId.toString())
            .flatMap { it?.document.toResultOr { IllegalStateException("PDF does not contain document") } }
}

sealed class GetLeastRecentlySignedPdfError : Exception() {
    object ConsentNotFound : GetLeastRecentlySignedPdfError()
    object SignedConsentNotFound : GetLeastRecentlySignedPdfError()
    object EmptyDocument : GetLeastRecentlySignedPdfError()
}
