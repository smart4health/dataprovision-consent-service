package com.healthmetrix.dynamicconsent.consent.views

import com.healthmetrix.dynamicconsent.consent.templating.ConsentPage
import com.healthmetrix.dynamicconsent.consent.templating.ReviewDocument
import kotlinx.html.DIV
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.iframe
import kotlinx.html.span

fun DIV.reviewPageView(page: ConsentPage.ReviewPage) {
    consentContainer(page) {
        consentHeader(page.title)

        div(classes = "consent-content-container") {
            button {
                id = "share-consent-button"
                +page.shareText
            }
            div(classes = "confirm-consent-document-container") {
                if (page.document != null) {
                    generateFromTemplate(page.document)
                } else if (page.source != null) {
                    wrapStaticHtmlInIframe(page.source)
                }
            }

            buttons(page) {
                backButton(page) {
                    classes += "review-back-button"
                    +page.backText
                }
                div {
                    id = "exit-consent"
                    attributes["data-redirect-url"] = page.nextPage
                    classes = setOf("consent-button", "next-button", "review-next-button")
                    +page.nextText
                }
            }
        }
    }
}

fun DIV.wrapStaticHtmlInIframe(source: String) {
    iframe(classes = "review-consent-frame") {
        src = source
    }
}

private fun DIV.generateFromTemplate(document: ReviewDocument) {
    div(classes = "confirm-consent-document-header") {
        span { +document.introduction.title }
    }

    div(classes = "confirm-consent-document-content") {
        div(classes = "consent-document-section") {
            wrapInUnsafeParagraph(document.introduction.content)
        }
    }

    document.information.forEach { section ->
        div(classes = "confirm-consent-document-header") {
            span { +section.title }
        }

        div(classes = "confirm-consent-document-content") {
            div(classes = "consent-document-section") {
                wrapInUnsafeParagraph(section.summary)
                section.description?.let {
                    wrapInUnsafeParagraph(section.description)
                }
            }
        }
    }

    document.options.forEachIndexed { index, section ->
        div(classes = "confirm-consent-document-header") {
            span { +section.title }
        }

        div(classes = "confirm-consent-document-content") {
            div(classes = "consent-document-section") {
                wrapInUnsafeParagraph(section.summary)
                section.description?.let {
                    wrapInUnsafeParagraph(section.description)
                }
            }
            div(classes = "review-question-container") {
                id = "dynamic-review-options"
                div(classes = "review-question-container-content") {
                    reviewOptionToggle(index, "accept", "review-option-accept-$index")
                    reviewOptionToggle(index, "reject", "review-option-reject-$index")
                    reviewOptionLabel(
                        "review-option-accept-$index",
                        "review-option-accept",
                        section.accept,
                    )
                    reviewOptionLabel(
                        "review-option-reject-$index",
                        "review-option-reject",
                        section.reject,
                    )
                }
            }
        }
    }

    document.glossary?.let { section ->
        div(classes = "confirm-consent-document-header") {
            span { +section.title }
        }
        div(classes = "confirm-consent-document-content") {
            section.items.forEach { item ->
                div(classes = "accordion") { +item.title }
                div("panel consent-document-section") {
                    wrapInUnsafeParagraph(item.text)
                }
            }
        }
    }
}
