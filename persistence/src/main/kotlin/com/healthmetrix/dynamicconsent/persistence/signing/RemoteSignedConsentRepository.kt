package com.healthmetrix.dynamicconsent.persistence.signing

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.healthmetrix.dynamicconsent.persistence.signing.api.SignedConsent
import com.healthmetrix.dynamicconsent.persistence.signing.api.SignedConsentRepository
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import com.github.michaelbull.result.runCatching as catch

@Component
@Profile("postgres")
class RemoteSignedConsentRepository(
    val remoteSignedConsentCrudRepository: RemoteSignedConsentCrudRepository,
) : SignedConsentRepository {
    override fun recordConsent(consent: SignedConsent): Result<Unit, Throwable> = catch {
        remoteSignedConsentCrudRepository.save(consent.toEntity())
    }

    override fun findById(consentFlowId: String): Result<SignedConsent, Throwable> = catch {
        remoteSignedConsentCrudRepository.findById(consentFlowId).get()
    }.map(SignedConsentEntity::toConsent)

    override fun findByDocumentId(documentId: String): Result<SignedConsent?, Throwable> = catch {
        remoteSignedConsentCrudRepository.findByDocumentId(documentId)?.toConsent()
    }
}

@Profile("postgres")
interface RemoteSignedConsentCrudRepository : CrudRepository<SignedConsentEntity, String> {
    fun findByDocumentId(documentId: String): SignedConsentEntity?
}
