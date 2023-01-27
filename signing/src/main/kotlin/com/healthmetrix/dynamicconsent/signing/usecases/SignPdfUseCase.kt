package com.healthmetrix.dynamicconsent.signing.usecases

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import com.healthmetrix.dynamicconsent.commons.decodeBase64
import com.healthmetrix.dynamicconsent.commons.pdf.SigningMaterial
import com.healthmetrix.dynamicconsent.commons.pdf.sign
import com.healthmetrix.dynamicconsent.commons.pdf.strategies.ConsentSigningStrategy
import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsentRepository
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlow
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlowRepository
import com.healthmetrix.dynamicconsent.persistence.signing.api.SignedConsent
import com.healthmetrix.dynamicconsent.persistence.signing.api.SignedConsentRepository
import com.healthmetrix.dynamicconsent.signing.SigningRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import javax.imageio.ImageIO

@Service
class SignPdfUseCase(
    private val cachedConsentRepository: CachedConsentRepository,
    private val signedConsentRepository: SignedConsentRepository,
    private val consentFlowRepository: ConsentFlowRepository,
    @Qualifier("signingSigningMaterial")
    private val signingMaterial: SigningMaterial,
    private val signingRepository: SigningRepository,
) {
    operator fun invoke(
        documentId: String,
        externalRefId: String,
        firstName: String,
        lastName: String,
        signature: String,
        metadata: Map<String, String>?,
        email: String? = null,
    ): Result<Unit, Throwable> = binding {
        val cachedConsent = cachedConsentRepository
            .findByDocumentId(documentId)
            .bind()
        val consentFlowId = cachedConsent!!.consentFlowId
        val consentId = cachedConsent.consentId
        val pdf = cachedConsent.pdf()

        val signatureImage = signature.removePrefix("data:image/png;base64,")
            .decodeBase64()
            .map(ByteArray::inputStream)
            .map(ImageIO::read)
            .bind()

        val pdfConfig = signingRepository.fetchConsentConfig(consentId).bind()
        val strategy = ConsentSigningStrategy(pdfConfig)

        val visibleSignatureOptions = strategy.sign(
            pdf,
            firstName,
            lastName,
            signatureImage,
        )

        consentFlowRepository.recordConsentFlow(
            ConsentFlow(
                consentFlowId = consentFlowId,
                externalRefId = externalRefId,
                consentId = consentId,
                signedAt = Date.from(Instant.now()),
            ),
        ).bind()

        signedConsentRepository.recordConsent(
            SignedConsent(
                documentId = documentId,
                consentFlowId = consentFlowId,
                firstName = firstName,
                familyName = lastName,
                document = pdf.sign(signingMaterial, visibleSignatureOptions),
                email = email,
                metadata = metadata,
            ),
        ).bind()
    }
}
