package com.healthmetrix.dynamicconsent.persistence.signing

import com.github.michaelbull.result.Result
import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsent
import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsentRepository
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import com.github.michaelbull.result.runCatching as catch

@Component
@Profile("postgres")
class RemoteCachedConsentRepository(
    val remoteCachedConsentCrudRepository: RemoteCachedConsentCrudRepository,
) : CachedConsentRepository {
    override fun save(consent: CachedConsent): Result<Unit, Throwable> = catch {
        remoteCachedConsentCrudRepository.save(consent.toEntity())
    }

    override fun findById(consentFlowId: String): CachedConsent? =
        remoteCachedConsentCrudRepository
            .findById(consentFlowId)
            .map(CachedConsentEntity::toConsent)
            .orElse(null)

    override fun findByDocumentId(documentId: String): Result<CachedConsent?, Throwable> = catch {
        remoteCachedConsentCrudRepository.findByDocumentId(documentId)?.toConsent()
    }
}

@Profile("postgres")
interface RemoteCachedConsentCrudRepository : CrudRepository<CachedConsentEntity, String> {
    fun findByDocumentId(documentId: String): CachedConsentEntity?
}
