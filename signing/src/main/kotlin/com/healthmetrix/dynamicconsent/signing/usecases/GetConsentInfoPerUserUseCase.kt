package com.healthmetrix.dynamicconsent.signing.usecases

import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlow
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlowRepository
import org.springframework.stereotype.Service
import java.util.Date

@Service
class GetConsentInfoPerUserUseCase(
    private val consentFlowRepository: ConsentFlowRepository,
) {
    operator fun invoke(externalRefId: String): Pair<List<ConsentInfo>, List<ConsentInfo>> =
        consentFlowRepository.findByExternalRefId(externalRefId)
            .asSequence()
            .filter { it.signedAt != null }
            .groupBy { it.consentId }
            .map { (_, signedConsentsPerId) ->
                // sort order: withdrawn ones, then after the signedAt Date. Take the most recent result as valid
                signedConsentsPerId
                    .sortedWith(compareBy({ it.withdrawnAt == null }, { it.signedAt }))
                    .last()
            }
            .map { it.toConsentInfo() }
            .partition { it.withdrawnAt == null }
}

data class ConsentInfo(
    val consentFlowId: String,
    val consentId: String,
    val signedAt: Date,
    val withdrawnAt: Date?,
)

fun ConsentFlow.toConsentInfo() = ConsentInfo(
    consentFlowId = consentFlowId,
    consentId = consentId,
    signedAt = signedAt!!,
    withdrawnAt = withdrawnAt,
)
