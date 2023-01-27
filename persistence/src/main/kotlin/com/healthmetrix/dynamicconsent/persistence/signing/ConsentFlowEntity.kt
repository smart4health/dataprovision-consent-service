package com.healthmetrix.dynamicconsent.persistence.signing

import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlow
import java.sql.Timestamp
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    name = "consent_flows",
    uniqueConstraints = [UniqueConstraint(columnNames = ["consentId", "externalRefId", "withdrawnAt"])],
)
class ConsentFlowEntity(
    @Id
    val consentFlowId: String,
    val consentId: String,
    val externalRefId: String,
    val signedAt: Timestamp? = null,
    val withdrawnAt: Timestamp? = null,
)

fun ConsentFlow.toEntity() =
    ConsentFlowEntity(
        consentFlowId = consentFlowId,
        consentId = consentId,
        externalRefId = externalRefId,
        signedAt = signedAt?.toInstant()?.let(Timestamp::from),
        withdrawnAt = withdrawnAt?.toInstant()?.let(Timestamp::from),
    )

fun ConsentFlowEntity.toConsent() = ConsentFlow(
    consentFlowId = consentFlowId,
    consentId = consentId,
    externalRefId = externalRefId,
    signedAt = signedAt,
    withdrawnAt = withdrawnAt,
)
