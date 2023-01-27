package com.healthmetrix.dynamicconsent.signing.usecases

import com.github.michaelbull.result.Result
import com.healthmetrix.dynamicconsent.signing.ConsentSigningTemplate
import com.healthmetrix.dynamicconsent.signing.SigningRepository
import org.springframework.stereotype.Service

@Service
class SelectSigningTemplateUseCase(private val signingRepository: SigningRepository) {
    operator fun invoke(consentId: String): Result<ConsentSigningTemplate, Throwable> =
        signingRepository.fetchTemplate(consentId)
}
