package com.healthmetrix.dynamicconsent.persistence.signing.api

import com.github.michaelbull.result.Result

interface SignedConsentRepository {
    fun recordConsent(consent: SignedConsent): Result<Unit, Throwable>
    fun findById(consentFlowId: String): Result<SignedConsent, Throwable>
    fun findByDocumentId(documentId: String): Result<SignedConsent?, Throwable>
}

data class SignedConsent(
    val consentFlowId: String,
    val documentId: String,
    val document: ByteArray? = null,
    val firstName: String,
    val familyName: String,
    val email: String? = null,
    val metadata: Map<String, Any>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignedConsent

        if (consentFlowId != other.consentFlowId) return false
        if (documentId != other.documentId) return false
        if (document != null) {
            if (other.document == null) return false
            if (!document.contentEquals(other.document)) return false
        } else if (other.document != null) return false
        if (firstName != other.firstName) return false
        if (familyName != other.familyName) return false
        if (email != other.email) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = consentFlowId.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + (document?.contentHashCode() ?: 0)
        result = 31 * result + firstName.hashCode()
        result = 31 * result + familyName.hashCode()
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (metadata?.hashCode() ?: 0)
        return result
    }
}
