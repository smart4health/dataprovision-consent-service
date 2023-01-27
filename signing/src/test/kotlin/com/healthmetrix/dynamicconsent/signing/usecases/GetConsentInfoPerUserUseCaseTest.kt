package com.healthmetrix.dynamicconsent.signing.usecases

import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlow
import com.healthmetrix.dynamicconsent.persistence.signing.api.ConsentFlowRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.UUID

class GetConsentInfoPerUserUseCaseTest {
    private val consentFlowRepository: ConsentFlowRepository = mockk()
    private val underTest = GetConsentInfoPerUserUseCase(consentFlowRepository)
    private val consentFlowId = "consentFlowId"
    private val externalRefId = "externalRefId"
    private val defaultConsentId = "consentId"
    private val fakeDate: Instant = LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()

    @Test
    fun `empty List`() {
        every { consentFlowRepository.findByExternalRefId(externalRefId) } returns emptyList()

        assertThat(underTest.invoke(externalRefId).first).isEmpty()
        assertThat(underTest.invoke(externalRefId).second).isEmpty()
    }

    @Test
    fun `list of unfinished consents should not be returned`() {
        every { consentFlowRepository.findByExternalRefId(externalRefId) } returns listOf(flow())

        assertThat(underTest.invoke(externalRefId).first).isEmpty()
        assertThat(underTest.invoke(externalRefId).second).isEmpty()
    }

    @Test
    fun `get valid consent out of three`() {
        every { consentFlowRepository.findByExternalRefId(externalRefId) } returns listOf(
            flow(signed = true, withdrawn = true),
            flow(signed = true, withdrawn = true),
            flow(id = consentFlowId, signed = true, withdrawn = false),
        )
        val result = underTest.invoke(externalRefId)
        assertThat(result.first).hasSize(1)
        assertThat(result.second).isEmpty()
        assertThat(result.first[0].consentFlowId).isEqualTo(consentFlowId)
    }

    @Test
    fun `test sorting on signedDate and multiple consentIds`() {
        every { consentFlowRepository.findByExternalRefId(externalRefId) } returns listOf(
            flow(
                signed = true,
                withdrawn = true,
                consentId = "consent1",
                signedDate = fakeDate.plusSeconds(100),
                id = "withdrawn1",
            ),
            flow(signed = true, withdrawn = true, consentId = "consent1", signedDate = fakeDate.plusSeconds(1)),
            flow(signed = true, consentId = "consent2", id = "consented"),
            flow(signed = true, withdrawn = true, consentId = "consent3", id = "withdrawn2"),
            flow(),
        )
        val result = underTest.invoke(externalRefId)
        assertThat(result.first).hasSize(1)
        assertThat(result.second).hasSize(2)
        assertThat(result.first[0].consentFlowId).isEqualTo("consented")
        assertThat(result.second[0].consentFlowId).isEqualTo("withdrawn1")
        assertThat(result.second[1].consentFlowId).isEqualTo("withdrawn2")
    }

    private fun flow(
        id: String = UUID.randomUUID().toString(),
        signed: Boolean = false,
        withdrawn: Boolean = false,
        consentId: String = defaultConsentId,
        signedDate: Instant = fakeDate,
    ): ConsentFlow = ConsentFlow(
        id,
        externalRefId,
        consentId,
        if (signed) Date.from(signedDate) else null,
        if (withdrawn) Date.from(signedDate.plusSeconds(60)) else null,
    )
}
