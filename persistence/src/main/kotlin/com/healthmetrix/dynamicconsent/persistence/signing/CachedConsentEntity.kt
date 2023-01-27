package com.healthmetrix.dynamicconsent.persistence.signing

import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsent
import java.sql.Timestamp
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "cached_consents")
class CachedConsentEntity(
    @Id
    val consentFlowId: String,
    val documentId: String,
    val consentId: String,
    val document: ByteArray,
    val createdAt: Timestamp,
)

fun CachedConsent.toEntity() = CachedConsentEntity(
    documentId = documentId,
    consentFlowId = consentFlowId,
    document = document,
    consentId = consentId,
    createdAt = Timestamp.from(createdAt.toInstant()),
)

fun CachedConsentEntity.toConsent() = CachedConsent(
    consentFlowId = consentFlowId,
    documentId = documentId,
    document = document,
    consentId = consentId,
    createdAt = createdAt,
)
