package com.healthmetrix.dynamicconsent.persistence.signing.api

import com.github.michaelbull.result.Result
import org.apache.pdfbox.pdmodel.PDDocument
import java.util.Date

interface CachedConsentRepository {
    fun save(consent: CachedConsent): Result<Unit, Throwable>
    fun findById(consentFlowId: String): CachedConsent?
    fun findByDocumentId(documentId: String): Result<CachedConsent?, Throwable>
}

class CachedConsent(
    val consentFlowId: String,
    val consentId: String,
    val documentId: String,
    val document: ByteArray,
    val createdAt: Date,
) {
    fun pdf(): PDDocument = PDDocument.load(this.document)
}
