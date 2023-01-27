package com.healthmetrix.dynamicconsent.consent.usecases

import com.github.michaelbull.result.Result
import com.healthmetrix.dynamicconsent.consent.ConsentRepository
import com.healthmetrix.dynamicconsent.consent.templating.ConsentTemplate
import org.springframework.stereotype.Service

@Service
class SelectTemplateUseCase(private val consentRepository: ConsentRepository) {
    operator fun invoke(consentId: String): Result<ConsentTemplate, Throwable> =
        consentRepository.fetchTemplate(consentId)
}
