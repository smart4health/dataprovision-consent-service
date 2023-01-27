package com.healthmetrix.dynamicconsent.signing.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsentRepository
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlowRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.Date

@Service
class WithdrawSignatureUseCase(
    private val consentFlowRepository: ConsentFlowRepository,
    private val cachedConsentRepository: CachedConsentRepository,
    private val clock: Clock,
) {
    operator fun invoke(externalRefId: String, consentId: String): Result<Unit, WithdrawSignatureError> = binding {
        val consentFlow =
            consentFlowRepository.findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(externalRefId, consentId)
                .mapError { WithdrawSignatureError.SignatureNotFound }
                .bind()
                .toResultOr { WithdrawSignatureError.SignatureNotFound }
                .bind()

        if (consentFlow.withdrawnAt != null) {
            Err(WithdrawSignatureError.SignatureAlreadyWithdrawn).bind<Unit>()
        }

        consentFlowRepository
            .recordConsentFlow(consentFlow.copy(withdrawnAt = Date.from(clock.instant())))
            .mapError { WithdrawSignatureError.UpdateWithdrawnAtFailed }
            .bind()
    }

    @Deprecated(
        "Use other invoke not using documentId, but consentId + externalRefId",
        replaceWith = ReplaceWith("WithdrawSignatureUseCase.invoke(java.lang.String, java.lang.String)"),
    )
    operator fun invoke(documentId: String): Result<Unit, WithdrawSignatureError> = binding {
        val cachedConsent = cachedConsentRepository.findByDocumentId(documentId)
            .mapError { WithdrawSignatureError.SignatureNotFound }
            .bind()
            .toResultOr { WithdrawSignatureError.SignatureNotFound }
            .bind()
        val consentFlow = consentFlowRepository.findById(cachedConsent.consentFlowId)
            .toResultOr { WithdrawSignatureError.SignatureNotFound }
            .bind()

        if (consentFlow.withdrawnAt != null) {
            Err(WithdrawSignatureError.SignatureAlreadyWithdrawn).bind<Unit>()
        }

        consentFlowRepository
            .recordConsentFlow(consentFlow.copy(withdrawnAt = Date.from(clock.instant())))
            .mapError { WithdrawSignatureError.UpdateWithdrawnAtFailed }
            .bind()
    }
}

sealed class WithdrawSignatureError : Exception() {
    object SignatureAlreadyWithdrawn : WithdrawSignatureError()
    object SignatureNotFound : WithdrawSignatureError()
    object UpdateWithdrawnAtFailed : WithdrawSignatureError()
}
