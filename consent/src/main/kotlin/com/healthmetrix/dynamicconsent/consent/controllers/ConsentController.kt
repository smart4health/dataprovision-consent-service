package com.healthmetrix.dynamicconsent.consent.controllers

import com.healthmetrix.dynamicconsent.commons.orThrow
import com.healthmetrix.dynamicconsent.consent.config.CONSENT_API_TAG
import com.healthmetrix.dynamicconsent.consent.usecases.LoadTemplateModelUseCase
import com.healthmetrix.dynamicconsent.consent.usecases.SelectTemplateUseCase
import com.healthmetrix.dynamicconsent.consent.views.ConsentView
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@Tag(name = CONSENT_API_TAG)
class ConsentController(
    private val loadTemplateModelUseCase: LoadTemplateModelUseCase,
    private val consentView: ConsentView,
    private val selectTemplateUseCase: SelectTemplateUseCase,
) {
    @GetMapping(path = ["/consents/{consentId}"])
    @Operation(
        summary = "Loads a dynamic consent template based on the consent ID",
    )
    @ResponseBody
    fun generateConsentFlow(
        @Parameter(
            description = "The identifier of the consent to load",
            example = "smart4health-research-consent-en",
        )
        @PathVariable
        consentId: String,
        @Parameter(
            description = "The URL to redirect to when the consent flow has successfully completed",
            example = "com.healthmetrix.researchapp://done",
        )
        @RequestParam
        successRedirectUrl: String,
        @Parameter(
            description = "The URL to redirect to when the consent flow is intentionally exited. For instance, if the user is not eligible to participate in the study",
            example = "com.healthmetrix.researchapp://ineligible",
        )
        @RequestParam(required = false)
        cancelRedirectUrl: String?,
        @Parameter(
            description = "Optional platform descriptor to load platform-specific styles",
            example = "ios",
        )
        @RequestParam(required = false)
        platform: String?,
    ): StringBuilder {
        return selectTemplateUseCase(consentId)
            .orThrow()
            .let { template ->
                loadTemplateModelUseCase(
                    template,
                    successRedirectUrl = successRedirectUrl,
                    cancelRedirectUrl = cancelRedirectUrl,
                    consentId = consentId,
                )
            }
            .let { templateModel -> consentView.build(consentId, templateModel, platform) }
    }
}
