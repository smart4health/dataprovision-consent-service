package com.healthmetrix.dynamicconsent.consent.usecases

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.healthmetrix.dynamicconsent.commons.pdf.SigningMaterial
import com.healthmetrix.dynamicconsent.commons.pdf.sign
import com.healthmetrix.dynamicconsent.commons.pdf.strategies.ConsentGenerationStrategy
import com.healthmetrix.dynamicconsent.consent.ConsentOption
import com.healthmetrix.dynamicconsent.consent.ConsentRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class GeneratePdfUseCase(
    @Qualifier("consentSigningMaterial")
    private val signingMaterial: SigningMaterial,
    private val consentRepository: ConsentRepository,
) {
    operator fun invoke(consentId: String, options: List<ConsentOption>): Result<ByteArray, Throwable> =
        binding {
            val pdf = consentRepository.fetchPdf(consentId).bind()
            val config = consentRepository.fetchConfig(consentId).bind()
            val strategy = ConsentGenerationStrategy({ pdf.inputStream() }, config)
            val map = options.map { it.optionId to it.consented }.toMap()

            strategy.generate(map).sign(signingMaterial)
        }
}
