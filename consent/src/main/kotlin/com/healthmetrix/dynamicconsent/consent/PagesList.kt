package com.healthmetrix.dynamicconsent.consent

import com.healthmetrix.dynamicconsent.commons.joinPaths
import com.healthmetrix.dynamicconsent.consent.templating.ConfirmConsent
import com.healthmetrix.dynamicconsent.consent.templating.ConsentPage
import com.healthmetrix.dynamicconsent.consent.templating.Defaults
import com.healthmetrix.dynamicconsent.consent.templating.Eligibility
import com.healthmetrix.dynamicconsent.consent.templating.Information
import com.healthmetrix.dynamicconsent.consent.templating.LearnMoreDisplayType
import com.healthmetrix.dynamicconsent.consent.templating.Option
import com.healthmetrix.dynamicconsent.consent.templating.PAGE_ID_PREFIX
import com.healthmetrix.dynamicconsent.consent.templating.Quiz
import com.healthmetrix.dynamicconsent.consent.templating.ReviewDocument
import com.healthmetrix.dynamicconsent.consent.templating.Welcome

internal class PagesList(private val staticResourcePath: String) {
    private val pages: MutableList<ConsentPage> = mutableListOf()
    private val previousPage: ConsentPage
        get() = pages.last()
    private val currentIndex: Int
        get() = pages.last().incrementPageIndex()
    private val nextIndex: Int
        get() = currentIndex + 1
    private val numOptions: Int
        get() = pages.count { page -> page is ConsentPage.OptionPage }

    fun addWelcome(welcome: Welcome, defaults: Defaults) {
        pages.add(
            ConsentPage.WelcomePage(
                title = welcome.title,
                imageUrl = welcome.image.asStatic(),
                content = welcome.description,
                nextText = welcome.navigation.next ?: defaults.navigation.next,
            ),
        )
    }

    fun addEligibility(eligibility: Eligibility, defaults: Defaults, cancelRedirectUrl: String?) {
        pages.add(
            ConsentPage.EligibilityIntroductionPage(
                title = eligibility.introduction.title,
                imageUrl = eligibility.introduction.image.asStatic(),
                content = eligibility.introduction.content,
                pageId = currentIndex.asPageId(),
                previousPage = previousPage.pageId,
                nextPage = nextIndex.asPageId(),
                nextText = eligibility.introduction.navigation.next ?: defaults.navigation.next,
                backText = eligibility.introduction.navigation.back ?: defaults.navigation.back,
            ),
        )
        pages.add(
            ConsentPage.EligibilityChecksPage(
                title = eligibility.checks.title,
                checks = eligibility.checks.checks,
                pageId = currentIndex.asPageId(),
                previousPage = previousPage.pageId,
                completedPage = (nextIndex + 1).asPageId(),
                failedPage = nextIndex.asPageId(),
                backText = eligibility.checks.navigation.back ?: defaults.navigation.back,
                nextText = eligibility.checks.navigation.next ?: defaults.navigation.next,
            ),
        )
        pages.add(
            ConsentPage.EligibilityFailedPage(
                title = eligibility.ineligible.title,
                content = eligibility.ineligible.content,
                imageUrl = eligibility.ineligible.image.asStatic(),
                pageId = currentIndex.asPageId(),
                redirectUrl = cancelRedirectUrl,
                exitText = eligibility.ineligible.navigation.next ?: defaults.navigation.next,
            ),
        )
        pages.add(
            ConsentPage.EligibilityCompletedPage(
                title = eligibility.completed.title,
                content = eligibility.completed.content,
                imageUrl = eligibility.completed.image.asStatic(),
                pageId = currentIndex.asPageId(),
                nextPage = nextIndex.asPageId(),
                nextText = eligibility.completed.navigation.next ?: defaults.navigation.next,
            ),
        )
    }

    fun addInformation(info: Information, defaults: Defaults, displayType: LearnMoreDisplayType) {
        val descriptionType = descriptionTypeOf(info.description, displayType)
        val previousPageIndex = when (previousPage) {
            is ConsentPage.DescriptionPage -> currentIndex - 2
            else -> previousPage.pageIndex()
        }
        // Next page should skip description page (+1) if it exists
        val nextPageIndex = if (descriptionType == LearnMoreDisplayType.PAGE) nextIndex + 1 else nextIndex

        pages.add(
            ConsentPage.SummaryPage(
                title = info.title,
                imageUrl = info.image.asStatic(),
                content = info.summary,
                pageId = currentIndex.asPageId(),
                nextPage = nextPageIndex.asPageId(),
                previousPage = previousPageIndex.asPageId(),
                descriptionPage = if (descriptionType == LearnMoreDisplayType.PAGE) nextIndex.asPageId() else null,
                descriptionModal = if (descriptionType == LearnMoreDisplayType.MODAL) info.description else null,
                nextText = info.navigation.next ?: defaults.navigation.next,
                backText = info.navigation.back ?: defaults.navigation.back,
                learnMoreText = info.navigation.learnMore ?: defaults.navigation.learnMore,
                closeText = info.navigation.close ?: defaults.navigation.close,
            ),
        )

        if (descriptionType == LearnMoreDisplayType.PAGE) {
            pages.add(
                ConsentPage.DescriptionPage(
                    title = info.title,
                    pageId = currentIndex.asPageId(),
                    nextPage = nextIndex.asPageId(),
                    previousPage = previousPage.pageId,
                    content = info.description,
                    nextText = info.navigation.next ?: defaults.navigation.next,
                    backText = info.navigation.back ?: defaults.navigation.back,
                ),
            )
        }
    }

    fun addOption(option: Option, defaults: Defaults, cancelRedirectUrl: String?, displayType: LearnMoreDisplayType) {
        val descriptionType = descriptionTypeOf(option.description, displayType)
        val previousPageIndex = when (previousPage) {
            is ConsentPage.DescriptionPage -> currentIndex - 2
            else -> previousPage.pageIndex()
        }
        // Next page should skip description page (+1) if it exists
        val nextPageIndex = if (descriptionType == LearnMoreDisplayType.PAGE) nextIndex + 1 else nextIndex

        pages.add(
            ConsentPage.OptionPage(
                title = option.title,
                pageId = currentIndex.asPageId(),
                content = option.summary,
                descriptionPage = if (descriptionType == LearnMoreDisplayType.PAGE) nextIndex.asPageId() else null,
                descriptionModal = if (descriptionType == LearnMoreDisplayType.MODAL) option.description else null,
                required = option.required,
                nextPage = nextPageIndex.asPageId(),
                previousPage = previousPageIndex.asPageId(),
                questionId = numOptions,
                accept = option.accept,
                reject = option.reject,
                nextText = option.navigation.next ?: defaults.navigation.next,
                backText = option.navigation.back ?: defaults.navigation.back,
                learnMoreText = option.navigation.learnMore ?: defaults.navigation.learnMore,
                closeText = option.navigation.close ?: defaults.navigation.close,
                popover = option.popover,
                redirectUrl = cancelRedirectUrl,
                requiredPopup = option.requiredPopup,
            ),
        )

        if (descriptionType == LearnMoreDisplayType.PAGE) {
            pages.add(
                ConsentPage.DescriptionPage(
                    title = option.title,
                    pageId = currentIndex.asPageId(),
                    content = option.description,
                    canContinue = false,
                    previousPage = previousPage.pageId,
                    nextPage = nextIndex.asPageId(),
                    nextText = option.navigation.next ?: defaults.navigation.next,
                    backText = option.navigation.back ?: defaults.navigation.back,
                ),
            )
        }
    }

    fun addQuiz(quiz: Quiz, defaults: Defaults) {
        pages.add(
            ConsentPage.QuizIntroductionPage(
                title = quiz.introduction.title,
                pageId = currentIndex.asPageId(),
                imageUrl = quiz.introduction.image.asStatic(),
                content = quiz.introduction.content,
                nextPage = nextIndex.asPageId(),
                previousPage = previousPage.pageId,
                nextText = quiz.introduction.navigation.next ?: defaults.navigation.next,
                backText = quiz.introduction.navigation.next ?: defaults.navigation.back,
            ),
        )

        quiz.questions.forEach { question ->
            pages.add(
                ConsentPage.QuizQuestionPage(
                    title = quiz.introduction.title,
                    pageId = currentIndex.asPageId(),
                    question = question.question,
                    answers = question.answers.map { answer ->
                        ConsentPage.QuizAnswer(
                            answer = answer.answer,
                            correct = answer.correct,
                        )
                    },
                    nextPage = nextIndex.asPageId(),
                    previousPage = previousPage.pageId,
                    quizExitPage = "quiz-failed",
                    nextText = question.navigation.next ?: defaults.navigation.next,
                    backText = question.navigation.back ?: defaults.navigation.back,
                    answerText = question.navigation.answer ?: defaults.navigation.answer,
                ),
            )
        }
    }

    fun addConfirm(confirmConsent: ConfirmConsent, defaults: Defaults) {
        pages.add(
            ConsentPage.ConfirmPage(
                pageId = currentIndex.asPageId(),
                title = confirmConsent.title,
                content = confirmConsent.content,
                nextPage = nextIndex.asPageId(),
                previousPage = previousPage.pageId,
                imageUrl = confirmConsent.image.asStatic(),
                nextText = confirmConsent.navigation.next ?: defaults.navigation.next,
                backText = confirmConsent.navigation.back ?: defaults.navigation.back,
            ),
        )
    }

    fun addReview(document: ReviewDocument, defaults: Defaults, successRedirectUrl: String) {
        pages.add(
            ConsentPage.ReviewPage(
                pageId = currentIndex.asPageId(),
                title = "Review Consent",
                document = document,
                previousPage = previousPage.pageId,
                nextPage = successRedirectUrl,
                nextText = defaults.navigation.next,
                backText = defaults.navigation.back,
                shareText = defaults.navigation.share,
            ),
        )
    }

    fun addReview(source: String, defaults: Defaults, successRedirectUrl: String) {
        pages.add(
            ConsentPage.ReviewPage(
                pageId = currentIndex.asPageId(),
                title = "Review Consent",
                source = source.asStatic(),
                previousPage = previousPage.pageId,
                nextPage = successRedirectUrl,
                nextText = defaults.navigation.next,
                backText = defaults.navigation.back,
                shareText = defaults.navigation.share,
            ),
        )
    }

    fun addQuizFailedPage(quiz: Quiz, defaults: Defaults, cancelRedirectUrl: String?) {
        pages.add(
            ConsentPage.QuizFailedPage(
                title = quiz.failed.title,
                pageId = "quiz-failed",
                content = quiz.failed.content,
                redirectUrl = cancelRedirectUrl,
                exitText = quiz.failed.navigation.next ?: defaults.navigation.next,
            ),
        )
    }

    fun toList() = pages.toList()

    private fun ConsentPage.pageIndex(): Int {
        return this.pageId.split("-").last().toInt()
    }

    private fun ConsentPage.incrementPageIndex() = this.pageIndex() + 1
    private fun Int.asPageId(): String = "$PAGE_ID_PREFIX-$this"
    private fun String.asStatic() = staticResourcePath.joinPaths(this)

    private fun descriptionTypeOf(description: String, displayType: LearnMoreDisplayType) =
        if (description.isBlank()) {
            LearnMoreDisplayType.NONE
        } else {
            displayType
        }
}
