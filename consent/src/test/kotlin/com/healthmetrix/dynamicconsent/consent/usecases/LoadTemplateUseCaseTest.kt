package com.healthmetrix.dynamicconsent.consent.usecases

import com.healthmetrix.dynamicconsent.consent.ConsentRepository
import com.healthmetrix.dynamicconsent.consent.fakeConsentTemplate
import com.healthmetrix.dynamicconsent.consent.fakeEligibility
import com.healthmetrix.dynamicconsent.consent.fakeQuiz
import com.healthmetrix.dynamicconsent.consent.templating.Backable
import com.healthmetrix.dynamicconsent.consent.templating.ConsentPage
import com.healthmetrix.dynamicconsent.consent.templating.GlossaryItem
import com.healthmetrix.dynamicconsent.consent.templating.GlossaryTemplate
import com.healthmetrix.dynamicconsent.consent.templating.PAGE_ID_PREFIX
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private class LoadTemplateUseCaseTest {
    private val consentRepository: ConsentRepository = mockk {
        every { staticPath(any(), any()) } returns "/fake/path"
    }
    private val underTest = LoadTemplateModelUseCase(consentRepository)
    private val fakeTemplate = fakeConsentTemplate
    private val consentId = "fake-consent"

    @Test
    fun `creates list of pages successfully`() {
        assertThat(
            underTest(
                template = fakeTemplate,
                successRedirectUrl = "fake redirect url",
                cancelRedirectUrl = "cancel redirect url",
                consentId = consentId,
            ).pages.size,
        ).isEqualTo(14)
    }

    @Test
    fun `creates list of pages successfully including glossary`() {
        assertThat(
            underTest(
                template = fakeTemplate.copy(
                    glossary = GlossaryTemplate(
                        items = listOf(
                            GlossaryItem(
                                "id",
                                "text",
                                null,
                            ),
                        ),
                        null,
                    ),
                ),
                successRedirectUrl = "fake redirect url",
                cancelRedirectUrl = "cancel redirect url",
                consentId = consentId,
            ).glossary,
        ).isNotNull
    }

    @Test
    fun `creates correct list of pages with eligibility`() {
        assertThat(
            underTest(
                template = fakeTemplate.copy(eligibility = fakeEligibility),
                successRedirectUrl = "fake redirect url",
                cancelRedirectUrl = "cancel redirect url",
                consentId = consentId,
            ).pages.size,
        ).isEqualTo(18)
    }

    @Test
    fun `creates correct list of pages with quiz`() {
        assertThat(
            underTest(
                template = fakeTemplate.copy(quiz = fakeQuiz),
                successRedirectUrl = "fake redirect url",
                cancelRedirectUrl = "cancel redirect url",
                consentId = consentId,
            ).pages.size,
        ).isEqualTo(18)
    }

    @Test
    fun `sets page id of Welcome to 0`() {
        val welcomePage =
            underTest(
                template = fakeTemplate,
                successRedirectUrl = "fake redirect url",
                cancelRedirectUrl = "cancel redirect url",
                consentId = consentId,
            ).pages.filterIsInstance<ConsentPage.WelcomePage>().first()
        assertThat(welcomePage.pageId).isEqualTo("$PAGE_ID_PREFIX-0")
    }

    @Test
    fun `creates eligibility intro page and points previous to welcome and next to checks`() {
        val pages =
            underTest(
                template = fakeTemplate.copy(eligibility = fakeEligibility),
                successRedirectUrl = "fake redirect url",
                cancelRedirectUrl = "cancel redirect url",
                consentId = consentId,
            ).pages
        val eligibilityIntro = pages.filterIsInstance<ConsentPage.EligibilityIntroductionPage>().first()
        assertThat(pages.goToPage(eligibilityIntro.previousPage)).isInstanceOf(ConsentPage.WelcomePage::class.java)
        assertThat(pages.goToPage(eligibilityIntro.nextPage)).isInstanceOf(ConsentPage.EligibilityChecksPage::class.java)
    }

    @Test
    fun `creates eligibility checks page and points previous to eligibility intro and next to eligibility completed`() {
        val pages =
            underTest(
                template = fakeTemplate.copy(eligibility = fakeEligibility),
                successRedirectUrl = "fake redirect url",
                cancelRedirectUrl = "cancel redirect url",
                consentId = consentId,
            ).pages
        val eligibilityChecks = pages.filterIsInstance<ConsentPage.EligibilityChecksPage>().first()
        assertThat(pages.goToPage(eligibilityChecks.previousPage)).isInstanceOf(ConsentPage.EligibilityIntroductionPage::class.java)
        assertThat(pages.goToPage(eligibilityChecks.completedPage)).isInstanceOf(ConsentPage.EligibilityCompletedPage::class.java)
        assertThat(pages.goToPage(eligibilityChecks.failedPage)).isInstanceOf(ConsentPage.EligibilityFailedPage::class.java)
    }

    @Test
    fun `creates eligibility completed page and next to summary`() {
        val pages =
            underTest(
                template = fakeTemplate.copy(eligibility = fakeEligibility),
                successRedirectUrl = "fake redirect url",
                cancelRedirectUrl = "cancel redirect url",
                consentId = consentId,
            ).pages
        val eligibilityCompleted = pages.filterIsInstance<ConsentPage.EligibilityCompletedPage>().first()
        assertThat(pages.goToPage(eligibilityCompleted.nextPage)).isInstanceOf(ConsentPage.SummaryPage::class.java)
        assertThat(eligibilityCompleted).isNotInstanceOf(Backable::class.java)
    }

    @Test
    fun `creates a summary for each information and description page for each information and option`() {
        val summaries = underTest(
            template = fakeTemplate,
            successRedirectUrl = "fake redirect url",
            cancelRedirectUrl = "cancel redirect url",
            consentId = consentId,
        ).pages.filterIsInstance<ConsentPage.SummaryPage>()
        val descriptions = underTest(
            template = fakeTemplate,
            successRedirectUrl = "fake redirect url",
            cancelRedirectUrl = "cancel redirect url",
            consentId = consentId,
        ).pages.filterIsInstance<ConsentPage.DescriptionPage>()

        assertThat(summaries.size).isEqualTo(fakeTemplate.information.size)
        assertThat(descriptions.size).isEqualTo(fakeTemplate.information.size + fakeTemplate.options.size)
    }

    @Test
    fun `Welcome points next to index 1`() {
        val welcomePage =
            underTest(
                template = fakeTemplate,
                cancelRedirectUrl = "fake redirect url",
                successRedirectUrl = "cancel redirect url",
                consentId = consentId,
            ).pages.filterIsInstance<ConsentPage.WelcomePage>().first()
        assertThat(welcomePage.nextPage).isEqualTo("$PAGE_ID_PREFIX-1")
    }

    @Test
    fun `points to next page on summary pages correctly as next non-description page`() {
        val pages = underTest(
            template = fakeTemplate,
            successRedirectUrl = "fake redirect url",
            cancelRedirectUrl = "cancel redirect url",
            consentId = consentId,
        ).pages
        val summaries = pages.filterIsInstance<ConsentPage.SummaryPage>()
        assertThat(pages.goToPage(summaries.first().nextPage))
            .isInstanceOf(ConsentPage.SummaryPage::class.java)
        assertThat(pages.goToPage(summaries[1].nextPage))
            .isInstanceOf(ConsentPage.SummaryPage::class.java)
        assertThat(pages.goToPage(summaries[2].nextPage))
            .isInstanceOf(ConsentPage.OptionPage::class.java)
    }

    @Test
    fun `points to previous page on summary pages correctly as previous non-description page`() {
        val pages =
            underTest(
                template = fakeTemplate.copy(eligibility = fakeEligibility),
                successRedirectUrl = "fake redirect url",
                cancelRedirectUrl = "cancel redirect url",
                consentId = consentId,
            ).pages
        val summaries = pages.filterIsInstance<ConsentPage.SummaryPage>()
        assertThat(pages.goToPage(summaries[0].previousPage)).isInstanceOf(ConsentPage.EligibilityCompletedPage::class.java)
        assertThat(pages.goToPage(summaries[1].previousPage)).isInstanceOf(ConsentPage.SummaryPage::class.java)
        assertThat(pages.goToPage(summaries[2].previousPage)).isInstanceOf(ConsentPage.SummaryPage::class.java)
    }

    @Test
    fun `description pages point next to next non-description-page`() {
        val pages = underTest(
            template = fakeTemplate,
            successRedirectUrl = "fake redirect url",
            cancelRedirectUrl = "cancel redirect url",
            consentId = consentId,
        ).pages
        val descriptions = pages.filterIsInstance<ConsentPage.DescriptionPage>()
        assertThat(pages.goToPage(descriptions[0].nextPage)).isInstanceOf(ConsentPage.SummaryPage::class.java)
        assertThat(pages.goToPage(descriptions[1].nextPage)).isInstanceOf(ConsentPage.SummaryPage::class.java)
        assertThat(pages.goToPage(descriptions[2].nextPage)).isInstanceOf(ConsentPage.OptionPage::class.java)
        assertThat(pages.goToPage(descriptions[3].nextPage)).isInstanceOf(ConsentPage.OptionPage::class.java)
        assertThat(pages.goToPage(descriptions[4].nextPage)).isInstanceOf(ConsentPage.OptionPage::class.java)
        assertThat(pages.goToPage(descriptions[5].nextPage)).isInstanceOf(ConsentPage.ReviewPage::class.java)
    }

    @Test
    fun `description pages point back to non-description pages`() {
        val pages = underTest(
            template = fakeTemplate,
            successRedirectUrl = "fake redirect url",
            cancelRedirectUrl = "cancel redirect url",
            consentId = consentId,
        ).pages
        val descriptions = pages.filterIsInstance<ConsentPage.DescriptionPage>()
        assertThat(pages.goToPage(descriptions[0].previousPage)).isInstanceOf(ConsentPage.SummaryPage::class.java)
        assertThat(pages.goToPage(descriptions[1].previousPage)).isInstanceOf(ConsentPage.SummaryPage::class.java)
        assertThat(pages.goToPage(descriptions[2].previousPage)).isInstanceOf(ConsentPage.SummaryPage::class.java)
        assertThat(pages.goToPage(descriptions[3].previousPage)).isInstanceOf(ConsentPage.OptionPage::class.java)
        assertThat(pages.goToPage(descriptions[4].previousPage)).isInstanceOf(ConsentPage.OptionPage::class.java)
        assertThat(pages.goToPage(descriptions[5].previousPage)).isInstanceOf(ConsentPage.OptionPage::class.java)
    }

    @Test
    fun `description pages linked to options cannot continue`() {
        val pages = underTest(
            template = fakeTemplate,
            successRedirectUrl = "fake redirect url",
            cancelRedirectUrl = "cancel redirect url",
            consentId = consentId,
        ).pages
        val descriptions = pages.filterIsInstance<ConsentPage.DescriptionPage>()
            .filter { page -> pages.goToPage(page.previousPage) is ConsentPage.OptionPage }
        descriptions.forEach { description -> assertThat(description.canContinue).isFalse() }
    }

    @Test
    fun `last option page points to quiz introduction page`() {
        val pages = underTest(
            template = fakeTemplate.copy(quiz = fakeQuiz),
            successRedirectUrl = "fake redirect url",
            cancelRedirectUrl = "cancel redirect url",
            consentId = consentId,
        ).pages
        val options = pages.filterIsInstance<ConsentPage.OptionPage>()

        assertThat(pages.goToPage(options.last().nextPage)).isInstanceOf(ConsentPage.QuizIntroductionPage::class.java)
    }

    @Test
    fun `quiz introduction links to quiz questions`() {
        val pages = underTest(
            template = fakeTemplate.copy(quiz = fakeQuiz),
            successRedirectUrl = "fake redirect url",
            cancelRedirectUrl = "cancel redirect url",
            consentId = consentId,
        ).pages
        val quizIntroPage = pages.filterIsInstance<ConsentPage.QuizIntroductionPage>()[0]

        assertThat(pages.goToPage(quizIntroPage.nextPage)).isInstanceOf(ConsentPage.QuizQuestionPage::class.java)
    }

    @Test
    fun `quiz question points to review page`() {
        val pages = underTest(
            template = fakeTemplate.copy(quiz = fakeQuiz),
            successRedirectUrl = "fake redirect url",
            cancelRedirectUrl = "cancel redirect url",
            consentId = consentId,
        ).pages
        val questionPages = pages.filterIsInstance<ConsentPage.QuizQuestionPage>()

        assertThat(pages.goToPage(questionPages.last().nextPage)).isInstanceOf(ConsentPage.ReviewPage::class.java)
    }

    fun List<ConsentPage>.goToPage(id: String): ConsentPage {
        return find { it.pageId == id }!!
    }
}
