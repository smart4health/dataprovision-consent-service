package com.healthmetrix.dynamicconsent.consent.views

import com.healthmetrix.dynamicconsent.commons.HTMLBuilder
import com.healthmetrix.dynamicconsent.commons.asLocalStaticResourcePath
import com.healthmetrix.dynamicconsent.consent.ConsentRepository
import com.healthmetrix.dynamicconsent.consent.templating.ConsentPage
import com.healthmetrix.dynamicconsent.consent.templating.RequiredPopup
import com.healthmetrix.dynamicconsent.consent.usecases.TemplateModel
import kotlinx.html.DIV
import kotlinx.html.a
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.unsafe
import org.springframework.stereotype.Service

@Service
class ConsentView(
    private val consentRepository: ConsentRepository,
) {
    fun build(consentId: String, templateModel: TemplateModel, platform: String?): StringBuilder =
        HTMLBuilder().apply {
            head {
                title = "Consent"
                stylesheets {
                    add("/styles/carousel.css".asLocalStaticResourcePath())
                    add("/styles/base.css".asLocalStaticResourcePath())
                    add("/styles/popover.css".asLocalStaticResourcePath())
                    add("/styles/modals.css".asLocalStaticResourcePath())
                    add("/styles/options.css".asLocalStaticResourcePath())
                    add("/styles/accordion.css".asLocalStaticResourcePath())
                    add(consentRepository.staticPath(consentId, "/styles/styles.css"))
                    platform?.let {
                        add(consentRepository.staticPath(consentId, "/styles/styles-$it.css"))
                    }
                }
            }
            scripts {
                add("/consent-bundle.js".asLocalStaticResourcePath())
            }
        }.build().buildView {
            div(classes = "consent-container") {
                div {
                    id = "carousel"
                    templateModel.pages.forEach { page ->
                        pageView(page)
                    }
                }
            }
            templateModel.glossary?.let {
                div(classes = "glossary") {
                    it.items.forEach { item ->
                        div(classes = "modal-learn-more") {
                            id = "glossary_${item.id}"
                            div(classes = "modal-learn-more-content container-shadow") {
                                div(classes = "modal-learn-more-header") {
                                    span("modal-learn-more-header-close") {
                                        id = "glossary_${item.id}_close1"
                                        unsafe { +"&times;" }
                                    }
                                }
                                div(classes = "modal-learn-more-body") { wrapInUnsafeParagraph(item.text) }
                                div(classes = "modal-learn-more-options") {
                                    div(classes = "modal-learn-more-option") {
                                        div(classes = "modal-learn-more-option-text") {
                                            id = "glossary_${item.id}_close2"
                                            +it.closeText
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

fun DIV.pageView(page: ConsentPage) {
    when (page) {
        is ConsentPage.WelcomePage -> welcomePageView(page)
        is ConsentPage.EligibilityIntroductionPage -> eligibilityIntroductionPageView(page)
        is ConsentPage.EligibilityChecksPage -> eligibilityChecksPageView(page)
        is ConsentPage.EligibilityCompletedPage -> eligibilityCompletedPageView(page)
        is ConsentPage.EligibilityFailedPage -> eligibilityFailedPageView(page)
        is ConsentPage.DescriptionPage -> descriptionPageView(page)
        is ConsentPage.SummaryPage -> summaryPageView(page)
        is ConsentPage.OptionPage -> optionPageView(page)
        is ConsentPage.QuizIntroductionPage -> quizIntroductionPageView(page)
        is ConsentPage.QuizQuestionPage -> quizQuestionPageView(page)
        is ConsentPage.QuizFailedPage -> quizFailedView(page)
        is ConsentPage.ConfirmPage -> confirmConsentView(page)
        is ConsentPage.ReviewPage -> reviewPageView(page)
    }
}

fun DIV.welcomePageView(page: ConsentPage.WelcomePage) {
    consentContainer(page) {
        consentHeader(page.title)
        consentImage(page.imageUrl)

        div(classes = "consent-content-container") {
            div(classes = "consent-content") {
                wrapInUnsafeParagraph(page.content)
            }
            buttons(page) {
                nextButton(page) {
                    classes += setOf("welcome-next-button")
                    +page.nextText
                }
            }
        }
    }
}

fun DIV.summaryPageView(page: ConsentPage.SummaryPage) {
    consentContainer(page) {
        consentHeader(page.title)

        if (page.imageUrl.isNotBlank()) {
            consentImage(page.imageUrl)
        }

        div(classes = "consent-content-container") {
            div(classes = "consent-content") {
                wrapInUnsafeParagraph(page.content)

                page.descriptionPage?.let { descriptionPage(page.descriptionPage, page.learnMoreText) }

                page.descriptionModal?.let { descriptionModalButton("description-${page.pageId}", page.learnMoreText) }
            }

            page.descriptionModal?.let {
                descriptionModal(
                    "description-${page.pageId}",
                    page.descriptionModal,
                    page.closeText,
                )
            }

            buttons(page) {
                backButton(page) {
                    classes += setOf("summary-back-button")
                    +page.backText
                }
                nextButton(page) {
                    classes += setOf("summary-next-button")
                    +page.nextText
                }
            }
        }
    }
}

fun DIV.descriptionPage(descriptionPageId: String, learnMoreText: String) {
    div(classes = "learn-more-container") {
        div(classes = "learn-more-button") {
            a(href = "#$descriptionPageId") {
                classes = setOf("carousel-transition")
                attributes["data-load-slide"] = descriptionPageId
                +learnMoreText
            }
        }
    }
}

fun DIV.descriptionModal(modalId: String, content: String, closeText: String) {
    div(classes = "modal-learn-more") {
        id = modalId
        div(classes = "modal-learn-more-content") {
            div(classes = "modal-learn-more-header") {
                span("modal-learn-more-header-close") {
                    id = "$modalId-close1"
                    unsafe { +"&times;" }
                }
            }
            div(classes = "modal-learn-more-body") { wrapInUnsafeParagraph(content) }
            div(classes = "modal-learn-more-options modal-description-options") {
                div(classes = "modal-learn-more-option modal-description-option") {
                    div(classes = "modal-learn-more-option-text modal-description-option-text") {
                        id = "$modalId-close2"
                        +closeText
                    }
                }
            }
        }
    }
}

fun DIV.descriptionModalButton(modalId: String, learnMoreText: String) {
    div(classes = "consent-description-options") {
        div(classes = "consent-description-option") {
            attributes["data-description-modal-id"] = modalId
            div(classes = "consent-description-option-text") {
                +learnMoreText
            }
        }
    }
}

fun DIV.descriptionPageView(page: ConsentPage.DescriptionPage) {
    consentContainer(page) {
        consentHeader(page.title)

        div(classes = "consent-content-container") {
            div(classes = "consent-content") {
                wrapInUnsafeParagraph(page.content)
            }

            buttons(page) {
                if (page.canGoBack) {
                    backButton(page) {
                        classes += "description-back-button"
                        +page.backText
                    }
                }
                if (page.canContinue) {
                    nextButton(page) {
                        classes += "description-next-button"
                        +page.nextText
                    }
                }
            }
        }
    }
}

fun DIV.optionPageView(page: ConsentPage.OptionPage) {
    consentContainer(page) {
        consentHeader(page.title)

        div(classes = "consent-content-container") {
            div(classes = "consent-content") {
                wrapInUnsafeParagraph(page.content)

                page.descriptionPage?.let { descriptionPage(page.descriptionPage, page.learnMoreText) }

                page.descriptionModal?.let { descriptionModalButton("description-${page.pageId}", page.learnMoreText) }
            }

            page.descriptionModal?.let {
                descriptionModal(
                    "description-${page.pageId}",
                    page.descriptionModal,
                    page.closeText,
                )
            }

            div(classes = "consent-question-container") {
                div(classes = "consent-question-container-content") {
                    id = "option-box-${page.questionId}"
                    attributes["data-bs-container"] = "body"
                    attributes["data-bs-toggle"] = "popover"
                    attributes["data-bs-placement"] = "bottom"
                    attributes["data-bs-content"] = page.popover
                    optionToggle(page, "accept", "option-accept-${page.questionId}")
                    optionToggle(page, "reject", "option-reject-${page.questionId}")
                    optionLabel("option-accept-${page.questionId}", "option-accept", page.accept)
                    optionLabel("option-reject-${page.questionId}", "option-reject", page.reject)
                }
            }

            buttons(page) {
                backButton(page) {
                    classes += "option-back-button"
                    +page.backText
                }
                nextButton(page) {
                    classes += "option-next-button"
                    attributes["data-question-id"] = page.questionId.toString()
                    if (page.required) {
                        attributes["data-required"] = page.required.toString()
                    }
                    page.redirectUrl?.let { attributes["data-cancel-redirect-url"] = page.redirectUrl }
                    +page.nextText
                }
            }

            page.requiredPopup?.let { buildRequiredRejectPopup(it, page.questionId) }
        }
    }
}

fun DIV.buildRequiredRejectPopup(popup: RequiredPopup, questionId: Int) {
    div(classes = "option-modal") {
        id = "option-$questionId-confirm-cancel-modal"
        div(classes = "option-modal-content") {
            p(classes = "option-cancel-modal-header center-text") { unsafe { +popup.title } }
            p(classes = "option-cancel-modal-body center-text") {
                unsafe { +popup.text }
            }
            div(classes = "option-modal-options") {
                div(classes = "option-modal-option option-modal-option-confirm") {
                    id = "option-$questionId-confirm-confirm-cancel-modal"
                    div(classes = "option-modal-option-text") {
                        unsafe { +popup.confirm }
                    }
                }
                div(classes = "option-modal-option option-modal-option-cancel") {
                    id = "option-$questionId-close-confirm-cancel-modal"
                    div(classes = "option-modal-option-text") {
                        unsafe { +popup.cancel }
                    }
                }
            }
        }
    }
}

fun DIV.confirmConsentView(page: ConsentPage.ConfirmPage) {
    consentContainer(page) {
        consentHeader(page.title)

        if (page.imageUrl.isNotBlank()) {
            consentImage(page.imageUrl)
        }

        div(classes = "consent-content-container") {
            div(classes = "consent-content") {
                wrapInUnsafeParagraph(page.content)
            }

            buttons(page) {
                backButton(page) {
                    classes += setOf("confirm-consent-back-button")
                    +"Back"
                }
                nextButton(page) {
                    classes += setOf("confirm-consent-next-button")
                    +"Review the Consent"
                }
            }
        }
    }
}
