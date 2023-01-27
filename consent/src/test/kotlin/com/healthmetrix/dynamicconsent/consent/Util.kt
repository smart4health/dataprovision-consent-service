package com.healthmetrix.dynamicconsent.consent

import com.healthmetrix.dynamicconsent.consent.templating.Config
import com.healthmetrix.dynamicconsent.consent.templating.ConsentTemplate
import com.healthmetrix.dynamicconsent.consent.templating.Defaults
import com.healthmetrix.dynamicconsent.consent.templating.Eligibility
import com.healthmetrix.dynamicconsent.consent.templating.EligibilityCheck
import com.healthmetrix.dynamicconsent.consent.templating.EligibilityChecks
import com.healthmetrix.dynamicconsent.consent.templating.EligibilityCompleted
import com.healthmetrix.dynamicconsent.consent.templating.EligibilityFailed
import com.healthmetrix.dynamicconsent.consent.templating.EligibilityIntroduction
import com.healthmetrix.dynamicconsent.consent.templating.Information
import com.healthmetrix.dynamicconsent.consent.templating.NavigationDefaults
import com.healthmetrix.dynamicconsent.consent.templating.Option
import com.healthmetrix.dynamicconsent.consent.templating.OptionNavigation
import com.healthmetrix.dynamicconsent.consent.templating.Quiz
import com.healthmetrix.dynamicconsent.consent.templating.QuizAnswer
import com.healthmetrix.dynamicconsent.consent.templating.QuizFailed
import com.healthmetrix.dynamicconsent.consent.templating.QuizIntroduction
import com.healthmetrix.dynamicconsent.consent.templating.QuizQuestion
import com.healthmetrix.dynamicconsent.consent.templating.RequiredPopup
import com.healthmetrix.dynamicconsent.consent.templating.Welcome

val fakeEligibility = Eligibility(
    introduction = EligibilityIntroduction(
        title = "Eligibility",
        image = "image.com",
        content = "Eligibility Welcome",
    ),
    checks = EligibilityChecks(
        title = "Eligibility Checks",
        checks = listOf(
            EligibilityCheck(check = "check 1", eligible = true),
            EligibilityCheck(check = "check 2", eligible = false),
            EligibilityCheck(check = "check 3", eligible = true),
        ),
    ),
    ineligible = EligibilityFailed(
        title = "Eligibility Failed",
        content = "Eligibility Failed",
        image = "image.com",
    ),
    completed = EligibilityCompleted(
        title = "Eligibility Completed",
        content = "Eligibility Completed",
        image = "image.com",
    ),
)

val fakeQuiz = Quiz(
    introduction = QuizIntroduction(title = "Quiz Intro", image = "image.test", content = "Welcome 2 Quiz"),
    questions = listOf(
        QuizQuestion(
            question = "Question 1",
            answers = listOf(QuizAnswer(answer = "answer 1", correct = true), QuizAnswer(answer = "answer 2")),
        ),
        QuizQuestion(
            question = "Question 2",
            answers = listOf(QuizAnswer(answer = "answer 2"), QuizAnswer(answer = "answer 2", correct = true)),
        ),
    ),
    failed = QuizFailed(title = "Quiz Failed", content = "Sorry you failed da quiz"),
)

/**
 * Represents a fake template loaded from the classpath.
 */
val fakeConsentTemplate = ConsentTemplate(
    consent = Config(
        version = "0.1",
        author = "healthmetrix",
    ),
    defaults = Defaults(
        navigation = NavigationDefaults(
            next = "Next",
            back = "Back",
            learnMore = "Learn More",
            answer = "Answer",
            exit = "Exit",
            share = "Share",
        ),
    ),
    welcome = Welcome(
        title = "Welcome",
        image = "image.com",
        description = "Welcome Description",
    ),
    eligibility = null,
    information = (1..3).map { num ->
        Information(
            title = "Info $num",
            image = "image.com",
            summary = "Info $num Summary",
            description = "Info $num description",
        )
    },
    options = (1..3).map { num ->
        Option(
            title = "Option $num",
            summary = "Option $num Summary",
            description = "Option $num Description",
            required = true,
            navigation = OptionNavigation(),
            requiredPopup = RequiredPopup(
                cancel = "cancel",
                confirm = "confirm",
                text = "text",
                title = "title",
            ),
        )
    },
    quiz = null,
    confirm = null,
    review = null,
    glossary = null,
)
