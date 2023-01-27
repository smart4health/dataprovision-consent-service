package com.healthmetrix.dynamicconsent.persistence.signing

import com.github.michaelbull.result.Result
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlow
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlowRepository
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import com.github.michaelbull.result.runCatching as catch

@Component
@Profile("postgres")
class RemoteConsentFlowRepository(
    val remoteConsentFlowCrudRepository: RemoteConsentFlowCrudRepository,
) : ConsentFlowRepository {
    override fun recordConsentFlow(consent: ConsentFlow): Result<Unit, Throwable> = catch {
        remoteConsentFlowCrudRepository.save(consent.toEntity())
    }

    override fun findById(consentFlowId: String): ConsentFlow? =
        remoteConsentFlowCrudRepository.findById(consentFlowId)
            .map(ConsentFlowEntity::toConsent)
            .orElse(null)

    override fun findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(
        externalRefId: String,
        consentId: String,
    ): Result<ConsentFlow?, Throwable> = catch {
        remoteConsentFlowCrudRepository.findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(externalRefId, consentId)
            ?.toConsent()
    }

    override fun findByExternalRefId(externalRefId: String): List<ConsentFlow> =
        remoteConsentFlowCrudRepository.findByExternalRefId(externalRefId).map { it.toConsent() }

    override fun findByExternalRefIdAndConsentId(
        externalRefId: String,
        consentId: String,
    ): List<ConsentFlow> =
        remoteConsentFlowCrudRepository.findByExternalRefIdAndConsentId(externalRefId, consentId).map { it.toConsent() }
}

@Profile("postgres")
interface RemoteConsentFlowCrudRepository : CrudRepository<ConsentFlowEntity, String> {
    @Query("SELECT c FROM ConsentFlowEntity c WHERE c.externalRefId = :externalRefId AND c.consentId = :consentId AND c.withdrawnAt is null")
    fun findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(
        externalRefId: String,
        consentId: String,
    ): ConsentFlowEntity?

    fun findByExternalRefIdAndConsentId(
        externalRefId: String,
        consentId: String,
    ): List<ConsentFlowEntity>

    fun findByExternalRefId(externalRefId: String): List<ConsentFlowEntity>
}
