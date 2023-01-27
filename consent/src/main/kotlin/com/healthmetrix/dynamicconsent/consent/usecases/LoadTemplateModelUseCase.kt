package com.healthmetrix.dynamicconsent.consent.usecases

import com.healthmetrix.dynamicconsent.consent.ConsentRepository
import com.healthmetrix.dynamicconsent.consent.PagesList
import com.healthmetrix.dynamicconsent.consent.templating.ConsentPage
import com.healthmetrix.dynamicconsent.consent.templating.ConsentTemplate
import com.healthmetrix.dynamicconsent.consent.templating.Glossary
import com.healthmetrix.dynamicconsent.consent.templating.ReviewDocument
import com.healthmetrix.dynamicconsent.consent.templating.ReviewType
import org.springframework.stereotype.Component

@Component
class LoadTemplateModelUseCase(
    private val consentRepository: ConsentRepository,
) {
    operator fun invoke(
        template: ConsentTemplate,
        successRedirectUrl: String,
        cancelRedirectUrl: String?,
        consentId: String,
    ): TemplateModel = TemplateModel(
        pages = PagesList(consentRepository.staticPath(consentId, ""))
            .apply {
                addWelcome(template.welcome, template.defaults)
                template.eligibility?.let { addEligibility(it, template.defaults, cancelRedirectUrl) }
                template.information.forEach {
                    addInformation(it, template.defaults, template.consent.learnMoreDisplayType)
                }
                template.options.forEach {
                    addOption(it, template.defaults, cancelRedirectUrl, template.consent.learnMoreDisplayType)
                }
                template.quiz?.let { addQuiz(it, template.defaults) }
                template.confirm?.let { addConfirm(it, template.defaults) }
                if (template.review?.type == ReviewType.EXTERNAL) {
                    addReview(
                        source = template.review.source,
                        successRedirectUrl = successRedirectUrl,
                        defaults = template.defaults,
                    )
                } else {
                    addReview(
                        document = ReviewDocument.fromTemplate(template),
                        successRedirectUrl = successRedirectUrl,
                        defaults = template.defaults,
                    )
                }
                template.quiz?.let { addQuizFailedPage(it, template.defaults, cancelRedirectUrl) }
            }.toList(),
        glossary = template.glossary?.items?.let { Glossary(template.defaults.navigation.close, items = it) },
    )
}

class TemplateModel(
    val pages: List<ConsentPage>,
    val glossary: Glossary?,
)
