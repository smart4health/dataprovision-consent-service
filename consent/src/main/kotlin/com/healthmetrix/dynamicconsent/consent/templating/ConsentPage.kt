package com.healthmetrix.dynamicconsent.consent.templating

import com.healthmetrix.dynamicconsent.commons.logger

const val PAGE_ID_PREFIX = "consent-page"

interface Backable {
    val previousPage: String
    val backText: String
}

interface Nextable {
    val nextPage: String
    val nextText: String
}

/**
 * These classes represent what the model would look like when rendered in the frontend
 */
sealed class ConsentPage {
    abstract val pageId: String
    abstract val title: String

    /**
     * The very first page the user sees and introduces them to the consent flow.
     */
    data class WelcomePage(
        override val pageId: String = "$PAGE_ID_PREFIX-0",
        override val title: String,
        val imageUrl: String,
        val content: String,
        override val nextPage: String = "$PAGE_ID_PREFIX-1",
        override val nextText: String,
    ) : ConsentPage(), Nextable

    /**
     * An Eligibility page to see if the user meets certain criteria before proceeding to the consent
     */
    data class EligibilityIntroductionPage(
        override val pageId: String,
        override val title: String,
        val imageUrl: String,
        val content: String,
        override val previousPage: String,
        override val nextPage: String,
        override val nextText: String,
        override val backText: String,
    ) : ConsentPage(), Backable, Nextable

    data class EligibilityChecksPage(
        override val pageId: String,
        override val title: String,
        val checks: List<EligibilityCheck>,
        override val previousPage: String,
        val completedPage: String,
        val failedPage: String,
        override val backText: String,
        val nextText: String,
    ) : ConsentPage(), Backable

    data class EligibilityFailedPage(
        override val pageId: String,
        override val title: String,
        val imageUrl: String,
        val content: String,
        val redirectUrl: String?,
        val exitText: String,
    ) : ConsentPage()

    data class EligibilityCompletedPage(
        override val pageId: String,
        override val title: String,
        val imageUrl: String,
        val content: String,
        override val nextPage: String,
        override val nextText: String,
    ) : ConsentPage(), Nextable

    /**
     * Introduces the user briefly to a concept,  but does not flood them with information. If the user wants to
     * read more, they can proceed to the `descriptionPage` to read more or the mutually exclusive `descriptionModal`.
     */
    data class SummaryPage(
        // Contains the PageId of the description page (or null if none should be displayed)
        val descriptionPage: String?,
        // Contains the content of the description modal (or null if none should be displayed)
        val descriptionModal: String?,
        override val title: String,
        val content: String,
        override val pageId: String,
        val imageUrl: String,
        override val nextPage: String,
        override val previousPage: String,
        override val nextText: String,
        override val backText: String,
        val learnMoreText: String,
        val closeText: String,
    ) : ConsentPage(), Backable, Nextable

    data class OptionPage(
        // Contains the PageId of the description page (or null if none should be displayed)
        val descriptionPage: String?,
        // Contains the content of the description modal (or null if none should be displayed)
        val descriptionModal: String?,
        override val title: String,
        val content: String,
        override val pageId: String,
        val questionId: Int,
        val required: Boolean = false,
        override val nextPage: String,
        override val previousPage: String,
        val accept: String,
        val reject: String,
        override val nextText: String,
        override val backText: String,
        val learnMoreText: String,
        val closeText: String,
        val popover: String,
        val redirectUrl: String?,
        val requiredPopup: RequiredPopup?,
    ) : ConsentPage(), Backable, Nextable

    /**
     * A Description page is the in-depth explanation of what was briefly introduced in the summary or option page.
     * It is its own Page and has the next/previous indexes adjusted.
     */
    data class DescriptionPage(
        override val title: String,
        val content: String,
        override val pageId: String,
        override val nextPage: String,
        override val previousPage: String,
        val canContinue: Boolean = true,
        val canGoBack: Boolean = true,
        override val nextText: String,
        override val backText: String,
    ) : ConsentPage(), Backable, Nextable

    /**
     * Quiz pages display a quiz for the user to test their understanding
     * of the material that was just presented to them
     */
    data class QuizIntroductionPage(
        override val title: String,
        override val pageId: String,
        val imageUrl: String,
        val content: String,
        override val nextPage: String,
        override val previousPage: String,
        override val nextText: String,
        override val backText: String,
    ) : ConsentPage(), Backable, Nextable

    data class QuizAnswer(val answer: String, val correct: Boolean)

    data class QuizQuestionPage(
        override val title: String,
        override val pageId: String,
        val question: String,
        val answers: List<QuizAnswer>,
        override val nextPage: String,
        override val previousPage: String,
        val quizExitPage: String,
        override val nextText: String,
        override val backText: String,
        val answerText: String,
    ) : ConsentPage(), Backable, Nextable

    data class QuizFailedPage(
        override val title: String,
        override val pageId: String,
        val content: String,
        val redirectUrl: String?,
        val exitText: String,
    ) : ConsentPage()

    data class ConfirmPage(
        override val title: String,
        override val pageId: String,
        override val nextPage: String,
        override val previousPage: String,
        val content: String,
        val imageUrl: String,
        override val nextText: String,
        override val backText: String,
    ) : ConsentPage(), Backable, Nextable

    /**
     * The Review Page is the final step in the flow, which compiles the options and selections
     * the user has made and displays them in an HTML-version of the consent document.
     */
    data class ReviewPage(
        override val pageId: String,
        override val title: String,
        val document: ReviewDocument? = null,
        val source: String? = null,
        override val previousPage: String,
        override val nextPage: String,
        override val nextText: String,
        override val backText: String,
        val shareText: String,
    ) : ConsentPage(), Backable, Nextable
}

data class ReviewDocument(
    val introduction: IntroductionSection,
    val information: List<InformationSection>,
    val options: List<OptionSection>,
    val glossary: GlossarySection?,
) {
    companion object {
        fun fromTemplate(template: ConsentTemplate): ReviewDocument {
            val introduction = IntroductionSection(template.welcome.title, template.welcome.description)
            val information = template.information.map {
                InformationSection(
                    title = it.title,
                    summary = it.summary,
                    description = it.description.ifBlank { null },
                )
            }
            val options = template.options.map {
                OptionSection(
                    title = it.title,
                    summary = it.summary,
                    description = it.description.ifBlank { null },
                    accept = it.accept,
                    reject = it.reject,
                )
            }
            val glossary = template.glossary?.let { it.toGlossarySection(it.config?.reviewPageTitle) }
            return ReviewDocument(introduction, information, options, glossary)
        }
    }
}

private fun GlossaryTemplate.toGlossarySection(reviewPageTitle: String?): GlossarySection? {
    if (reviewPageTitle.isNullOrBlank()) {
        logger.debug("Skipping ReviewPage Glossary: no title found")
        return null
    }
    val items = items.mapNotNull { item -> item.config?.reviewPageTitle?.let { GlossarySection.Item(it, item.text) } }
    if (items.isEmpty()) {
        logger.warn("Skipping ReviewPage Glossary: no eligible items found having a title")
        return null
    }
    return GlossarySection(reviewPageTitle, items)
}

data class IntroductionSection(val title: String, val content: String)
data class InformationSection(val title: String, val summary: String, val description: String?)
data class OptionSection(
    val title: String,
    val summary: String,
    val description: String?,
    val accept: String,
    val reject: String,
)

data class GlossarySection(
    val title: String,
    val items: List<Item>,
) {
    data class Item(
        val title: String,
        val text: String,
    )
}
