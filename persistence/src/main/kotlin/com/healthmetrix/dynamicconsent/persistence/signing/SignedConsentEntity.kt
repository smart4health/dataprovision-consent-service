@file:Suppress("DEPRECATION")

package com.healthmetrix.dynamicconsent.persistence.signing

import com.healthmetrix.dynamicconsent.persistence.signing.api.SignedConsent
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@TypeDefs(
    TypeDef(name = "jsonb", typeClass = JsonBinaryType::class),
)
@Table(name = "signed_consents")
class SignedConsentEntity(
    @Id
    val consentFlowId: String,
    val documentId: String,
    val document: ByteArray? = null,
    val firstName: String,
    val familyName: String,
    val email: String? = null,
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    val metadata: Map<String, Any>? = null,
)

fun SignedConsent.toEntity() =
    SignedConsentEntity(
        consentFlowId = consentFlowId,
        documentId = documentId,
        firstName = firstName,
        familyName = familyName,
        document = document,
        metadata = metadata,
        email = email,
    )

fun SignedConsentEntity.toConsent() = SignedConsent(
    consentFlowId = consentFlowId,
    documentId = documentId,
    firstName = firstName,
    familyName = familyName,
    document = document,
    metadata = metadata,
    email = email,
)
