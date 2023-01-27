package com.healthmetrix.dynamicconsent.signing.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsent
import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsentRepository
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlow
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlowRepository
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class WithdrawSignatureUseCaseTest {
    private val consentFlowRepository: ConsentFlowRepository = mockk()
    private val cachedConsentRepository: CachedConsentRepository = mockk()
    private val fakeDate: Instant = LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()
    private val fixedClock: Clock = Clock.fixed(fakeDate, ZoneId.systemDefault())
    private val consentId = "test-institution-lang"
    private val consentFlowId = "some-user"
    private val externalRefId = "extRef"
    private val documentId = "someDocumentId"
    private val underTest = WithdrawSignatureUseCase(consentFlowRepository, cachedConsentRepository, fixedClock)

    @Test
    fun `it marks consent as withdrawn v1`() {
        val cachedFlow = CachedConsent(
            consentFlowId = consentFlowId,
            consentId = consentId,
            documentId = documentId,
            createdAt = Date.from(fakeDate),
            document = ByteArray(0),
        )
        every { cachedConsentRepository.findByDocumentId(documentId) } returns Ok(cachedFlow)
        val consentFlow = ConsentFlow(
            consentFlowId = consentFlowId,
            signedAt = Date.from(Instant.now()),
            consentId = consentId,
            externalRefId = externalRefId,
        )
        every { consentFlowRepository.findById(consentFlowId) } returns consentFlow
        every { consentFlowRepository.recordConsentFlow(any()) } returns Ok(Unit)
        underTest(documentId = documentId)

        verify {
            consentFlowRepository.recordConsentFlow(
                consentFlow.copy(
                    withdrawnAt = Date.from(fakeDate),
                ),
            )
            // mockk has a stricter verification that requires all mocks to be verified before confirming
            consentFlowRepository.findById(consentFlowId)
        }
        confirmVerified(consentFlowRepository)
    }

    @Test
    fun `it errors if consent is already withdrawn v1`() {
        val cachedFlow = CachedConsent(
            consentFlowId = consentFlowId,
            consentId = consentId,
            documentId = documentId,
            createdAt = Date.from(fakeDate),
            document = ByteArray(0),
        )
        every { cachedConsentRepository.findByDocumentId(documentId) } returns Ok(cachedFlow)
        val consentFlow = ConsentFlow(
            consentFlowId = consentFlowId,
            signedAt = Date.from(Instant.now()),
            withdrawnAt = Date.from(Instant.now()),
            consentId = consentId,
            externalRefId = externalRefId,
        )
        every { consentFlowRepository.findById(consentFlowId) } returns consentFlow

        val result = underTest(documentId = documentId)
        assertThat(result).isInstanceOf(Err::class.java)
    }

    @Test
    fun `it errors if consent is not found v1`() {
        every { cachedConsentRepository.findByDocumentId(any()) } returns Err(
            Error("no signature"),
        )
        every { consentFlowRepository.findById(any()) } returns null
        val result = underTest(documentId = documentId)
        assertThat(result).isInstanceOf(Err::class.java)
    }

    @Test
    fun `it marks consent as withdrawn v2`() {
        val consentFlow = ConsentFlow(
            consentFlowId = consentFlowId,
            signedAt = Date.from(Instant.now()),
            consentId = consentId,
            externalRefId = externalRefId,
        )
        every {
            consentFlowRepository.findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(
                externalRefId,
                consentId,
            )
        } returns Ok(consentFlow)
        every { consentFlowRepository.recordConsentFlow(any()) } returns Ok(Unit)
        underTest(externalRefId = externalRefId, consentId = consentId)
        verify {
            consentFlowRepository.recordConsentFlow(
                consentFlow.copy(
                    withdrawnAt = Date.from(fakeDate),
                ),
            )
            // mockk has a stricter verification that requires all mocks to be verified before confirming
            consentFlowRepository.findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(
                externalRefId,
                consentId,
            )
        }
        confirmVerified(consentFlowRepository)
    }

    @Test
    fun `it errors if consent is already withdrawn v2`() {
        val consentFlow = ConsentFlow(
            consentFlowId = consentFlowId,
            signedAt = Date.from(Instant.now()),
            withdrawnAt = Date.from(Instant.now()),
            consentId = consentId,
            externalRefId = externalRefId,
        )
        every {
            consentFlowRepository.findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(
                externalRefId,
                consentId,
            )
        } returns Ok(
            consentFlow,
        )

        val result = underTest(externalRefId = externalRefId, consentId = consentId)
        assertThat(result).isInstanceOf(Err::class.java)
    }

    @Test
    fun `it errors if consent is not found v2`() {
        every { consentFlowRepository.findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(any(), any()) } returns Ok(
            null,
        )
        val result = underTest(externalRefId = externalRefId, consentId = consentId)
        assertThat(result).isInstanceOf(Err::class.java)
    }
}
