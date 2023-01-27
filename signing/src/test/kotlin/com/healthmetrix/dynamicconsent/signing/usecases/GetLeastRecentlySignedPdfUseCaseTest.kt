package com.healthmetrix.dynamicconsent.signing.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlow
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlowRepository
import com.healthmetrix.dynamicconsent.persistence.signing.api.SignedConsent
import com.healthmetrix.dynamicconsent.persistence.signing.api.SignedConsentRepository
import com.healthmetrix.dynamicconsent.signing.DocumentID
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date

class GetLeastRecentlySignedPdfUseCaseTest {
    private val signedConsentRepository: SignedConsentRepository = mockk()
    private val consentFlowRepository: ConsentFlowRepository = mockk()
    private val underTest = GetLeastRecentlySignedPdfUseCase(signedConsentRepository, consentFlowRepository)
    private val document = "fake Document".byteInputStream()
    private val consentFlowId = "consentFlowId"
    private val externalRefId = "externalRefId"
    private val consentId = "consentId"
    private val documentId = DocumentID.randomUUID()
    private val documentIdToString = documentId.toString()
    private val now = Date.from(Instant.now())

    private val signedConsent = SignedConsent(
        consentFlowId = consentFlowId,
        documentId = documentIdToString,
        document = document.readBytes(),
        firstName = "Lollipus",
        familyName = "McKotlin",
    )

    private val consentFlow = ConsentFlow(
        consentFlowId = consentFlowId,
        consentId = consentId,
        externalRefId = externalRefId,
    )

    @Nested
    inner class V1Tests {
        @Test
        fun `it attempts to fetch a signed consent with documentId`() {
            every { signedConsentRepository.findByDocumentId(any()) } returns Ok(signedConsent)
            underTest(externalRefId, documentId)
            verify { signedConsentRepository.findByDocumentId(documentIdToString) }
            confirmVerified(signedConsentRepository)
        }

        @Test
        fun `it returns Ok when successfully finding signed consent`() {
            every { signedConsentRepository.findByDocumentId(any()) } returns Ok(signedConsent)
            assertThat(underTest(externalRefId, documentId)).isInstanceOf(Ok::class.java)
        }

        @Test
        fun `it throws when finding errors`() {
            every { signedConsentRepository.findByDocumentId(any()) } returns Err(Exception("lol"))
            assertThat(underTest(externalRefId, documentId)).isInstanceOf(Err::class.java)
        }
    }

    @Nested
    inner class V2Tests {
        @Test
        fun `it attempts to fetch a signed consent with consentFlowId`() {
            every {
                consentFlowRepository.findByExternalRefIdAndConsentId(
                    externalRefId,
                    consentId,
                )
            } returns listOf(consentFlow)
            every { signedConsentRepository.findById(consentFlowId) } returns Ok(signedConsent)
            underTest(externalRefId, consentId)
            verify { signedConsentRepository.findById(consentFlowId) }
            confirmVerified(signedConsentRepository)
        }

        @Test
        fun `check order to get the currently valid consent`() {
            val firstWithdrawn = consentFlow.copy(
                consentFlowId = "First Withdrawn",
                signedAt = Date.from(now.toInstant().minusSeconds(501)),
                withdrawnAt = Date.from(now.toInstant().minusSeconds(500)),
            )
            val secondWithdrawn = consentFlow.copy(
                consentFlowId = "Second Withdrawn",
                signedAt = Date.from(now.toInstant().minusSeconds(301)),
                withdrawnAt = Date.from(now.toInstant().minusSeconds(300)),
            )
            val current = consentFlow.copy(
                consentFlowId = "current",
                signedAt = now,
                withdrawnAt = null,
            )
            every {
                consentFlowRepository.findByExternalRefIdAndConsentId(
                    externalRefId,
                    consentId,
                )
            } returns listOf(
                firstWithdrawn,
                current,
                secondWithdrawn,
            )
            every { signedConsentRepository.findById(current.consentFlowId) } returns Ok(signedConsent)
            underTest(externalRefId, consentId)
            verify { signedConsentRepository.findById(current.consentFlowId) }
            confirmVerified(signedConsentRepository)
        }

        @Test
        fun `check order to get the least recently withdrawn doc if theres no valid one`() {
            val firstWithdrawn = consentFlow.copy(
                consentFlowId = "First Withdrawn",
                signedAt = Date.from(now.toInstant().minusSeconds(501)),
                withdrawnAt = Date.from(now.toInstant().minusSeconds(500)),
            )
            val secondWithdrawn = consentFlow.copy(
                consentFlowId = "Second Withdrawn",
                signedAt = Date.from(now.toInstant().minusSeconds(301)),
                withdrawnAt = Date.from(now.toInstant().minusSeconds(300)),
            )
            val leastRecentlyWithdrawn = consentFlow.copy(
                consentFlowId = "Third Withdrawn",
                signedAt = now,
                withdrawnAt = now,
            )
            every {
                consentFlowRepository.findByExternalRefIdAndConsentId(
                    externalRefId,
                    consentId,
                )
            } returns listOf(
                firstWithdrawn,
                leastRecentlyWithdrawn,
                secondWithdrawn,
            )
            every { signedConsentRepository.findById(leastRecentlyWithdrawn.consentFlowId) } returns Ok(signedConsent)
            underTest(externalRefId, consentId)
            verify { signedConsentRepository.findById(leastRecentlyWithdrawn.consentFlowId) }
            confirmVerified(signedConsentRepository)
        }

        @Test
        fun `throws when finding errors`() {
            every {
                consentFlowRepository.findByExternalRefIdAndConsentId(
                    externalRefId,
                    consentId,
                )
            } returns emptyList()
            assertThat(underTest(externalRefId, consentId)).isInstanceOf(Err::class.java)
        }
    }
}
