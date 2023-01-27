package com.healthmetrix.dynamicconsent.consent.views

import com.healthmetrix.dynamicconsent.consent.CancelUrl
import com.healthmetrix.dynamicconsent.consent.templating.ConsentPage
import kotlinx.html.DIV
import kotlinx.html.a
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.radioInput

fun DIV.eligibilityIntroductionPageView(page: ConsentPage.EligibilityIntroductionPage) {
    consentContainer(page) {
        consentHeader(page.title)
        consentImage(page.imageUrl)
        div(classes = "consent-content-container") {
            div(classes = "consent-content") {
                wrapInUnsafeParagraph(page.content)
            }

            buttons(page) {
                backButton(page) {
                    classes += "eligibility-intro-back-button"
                    +page.backText
                }
                nextButton(page) {
                    classes += "eligibility-intro-next-button"
                    +page.nextText
                }
            }
        }
    }
}

fun DIV.eligibilityChecksPageView(page: ConsentPage.EligibilityChecksPage) {
    consentContainer(page) {
        id = "eligibility-container"
        consentHeader(page.title)
        div(classes = "consent-content-container") {
            div(classes = "consent-content eligibility-wrapper") {
                page.checks.forEachIndexed { index, check ->
                    div(classes = "eligibility-container") {
                        div(classes = "eligibility-text-container") {
                            wrapInUnsafeParagraph(check.check)
                        }
                        div(classes = "eligibility-check-container") {
                            div(classes = "eligibility-input-container") {
                                div(classes = "eligibility-input-text") { +"Yes" }
                                div(classes = "eligibility-radio-container") {
                                    radioInput(name = "eligibility-check-$index", classes = "eligibility-radio") {
                                        if (check.eligible) attributes["data-eligible"] = "true"
                                        value = "yes"
                                    }
                                }
                            }
                            div(classes = "eligibility-input-container") {
                                div(classes = "eligibility-input-text") { +"No" }
                                div(classes = "eligibility-radio-container") {
                                    radioInput(name = "eligibility-check-$index", classes = "eligibility-radio") {
                                        if (!check.eligible) attributes["data-eligible"] = "true"
                                        value = "no"
                                    }
                                }
                            }
                        }
                    }
                }
            }

            buttons(page) {
                backButton(page) {
                    classes += "eligibility-check-back-button"
                    +page.backText
                }
                a(href = "#${page.completedPage}") {
                    classes =
                        setOf("carousel-transition", "consent-button", "next-button", "eligibility-check-next-button")
                    id = "eligibility-check-next"
                    attributes["data-load-slide"] = page.completedPage
                    +page.nextText
                }
                a(href = "#${page.failedPage}") {
                    classes =
                        setOf("carousel-transition", "consent-button", "next-button", "eligibility-check-failed-button")
                    id = "eligibility-check-failed"
                    attributes["data-load-slide"] = page.failedPage
                    +page.nextText
                }
            }
        }
    }
}

fun DIV.eligibilityCompletedPageView(page: ConsentPage.EligibilityCompletedPage) {
    consentContainer(page) {
        consentHeader(page.title)
        consentImage(page.imageUrl)
        div(classes = "consent-content-container") {
            div(classes = "consent-content") {
                wrapInUnsafeParagraph(page.content)
            }

            buttons(page) {
                nextButton(page) {
                    classes += "eligibility-completed-next-button"
                    +"Next"
                }
            }
        }
    }
}

fun DIV.eligibilityFailedPageView(page: ConsentPage.EligibilityFailedPage) {
    consentContainer(page) {
        consentHeader(page.title)
        consentImage(page.imageUrl)
        div(classes = "consent-content-container") {
            div(classes = "consent-content") {
                wrapInUnsafeParagraph(page.content)
            }

            buttons(page) {
                a(href = page.redirectUrl?.let { url -> CancelUrl(url).eligibility }) {
                    classes = setOf("consent-button", "back-button")
                    id = "eligibility-exit-button"
                    +"Exit"
                }
            }
        }
    }
}
