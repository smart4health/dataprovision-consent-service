package com.healthmetrix.dynamicconsent.consent.views

import com.healthmetrix.dynamicconsent.consent.CancelUrl
import com.healthmetrix.dynamicconsent.consent.templating.ConsentPage
import kotlinx.html.DIV
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.h4
import kotlinx.html.input
import kotlinx.html.p

fun DIV.quizIntroductionPageView(page: ConsentPage.QuizIntroductionPage) {
    consentContainer(page) {
        consentHeader(page.title)
        consentImage(page.imageUrl)

        div(classes = "consent-content-container") {
            div(classes = "consent-content") {
                wrapInUnsafeParagraph(page.content)
            }

            buttons(page) {
                backButton(page) {
                    classes += "quiz-intro-back-button"
                    +page.backText
                }
                nextButton(page) {
                    classes += "quiz-intro-next-button"
                    +page.nextText
                }
            }
        }
    }
}

fun DIV.quizQuestionPageView(page: ConsentPage.QuizQuestionPage) {
    consentContainer(page) {
        div(classes = "consent-content-container consent-quiz-container") {
            div(classes = "consent-quiz-question-container") {
                h4 { +page.question }
            }
            div(classes = "consent-quiz-answer-message-container consent-quiz-incorrect-answer-container hidden") {
                h4 { +"Your answer was incorrect" }
                p { +"Please read the question and answers carefully and try again." }
            }
            div(classes = "consent-quiz-answer-message-container consent-quiz-correct-answer-container hidden") {
                h4 { +"Your answer was correct" }
                p { +"Let's move on" }
            }
            div(classes = "consent-quiz-answers-container") {
                page.answers.forEach { answer ->
                    answer(answer, page.pageId)
                }
            }
            buttons(page) {
                backButton(page) {
                    classes += "quiz-question-back-button"
                    +page.backText
                }

                a(href = "#") {
                    classes = setOf("quiz-question-answer-button", "consent-button", "next-button")
                    attributes["data-exit-page"] = page.quizExitPage
                    +page.answerText
                }

                nextButton(page) {
                    classes += setOf("quiz-question-next-button", "hidden")
                    +page.nextText
                }
            }
        }
    }
}

fun DIV.quizFailedView(page: ConsentPage.QuizFailedPage) {
    consentContainer(page) {
        consentHeader(page.title)
        div(classes = "consent-content-container") {
            div(classes = "consent-content") {
                wrapInUnsafeParagraph(page.content)
            }

            buttons(page) {
                a(href = page.redirectUrl?.let { url -> CancelUrl(url).quiz }) {
                    classes = setOf("quiz-failed-exit-button", "consent-button", "next-button")
                    +page.exitText
                }
            }
        }
    }
}
fun DIV.answer(answer: ConsentPage.QuizAnswer, radioGroupId: String) {
    div(classes = "consent-quiz-answer-container") {
        div(classes = "consent-quiz-answer-checkbox") {
            input(type = InputType.radio, classes = "consent-quiz-answer", name = radioGroupId) {
                if (answer.correct) {
                    attributes["data-answer-correct"] = answer.correct.toString()
                }
            }
        }
        div(classes = "consent-quiz-answer-text") {
            +answer.answer
        }
    }
}
