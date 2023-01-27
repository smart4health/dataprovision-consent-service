package com.healthmetrix.dynamicconsent.signing.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.dynamicconsent.persistence.signing.api.CachedConsentRepository
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlow
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlowRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CacheDocumentUseCaseTest {
    private val cachedConsentRepository: CachedConsentRepository = mockk()
    private val consentFlowRepository: ConsentFlowRepository = mockk()
    private val externalRefId = "externalRefId"
    private val consentId = "consent id"
    private val underTest = CacheDocumentUseCase(cachedConsentRepository, consentFlowRepository)
    private val document = "Fake Document".toByteArray()

    @Test
    fun `it returns Ok when successful`() {
        every { cachedConsentRepository.save(any()) } returns Ok(Unit)
        every { consentFlowRepository.recordConsentFlow(any()) } returns Ok(Unit)
        every {
            consentFlowRepository.findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(any(), any())
        } returns Ok(null)
        val result = underTest(externalRefId, document, consentId)
        assertThat(result).isInstanceOf(Ok::class.java)
    }

    @Test
    fun `it returns Err when upload returns false`() {
        every {
            consentFlowRepository.findByExternalRefIdAndConsentIdAndWithdrawnAtIsNull(any(), any())
        } returns Ok(ConsentFlow("id", externalRefId, consentId))
        every { consentFlowRepository.recordConsentFlow(any()) } returns Err(Throwable("Failed"))
        val result = underTest(externalRefId, document, consentId)
        assertThat(result).isInstanceOf(Err::class.java)
    }
}
