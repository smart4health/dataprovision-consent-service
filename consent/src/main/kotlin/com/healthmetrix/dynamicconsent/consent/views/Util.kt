package com.healthmetrix.dynamicconsent.consent.views
import com.healthmetrix.dynamicconsent.consent.templating.Backable
import com.healthmetrix.dynamicconsent.consent.templating.ConsentPage
import com.healthmetrix.dynamicconsent.consent.templating.Nextable
import kotlinx.html.A
import kotlinx.html.DIV
import kotlinx.html.a
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.radioInput
import kotlinx.html.span
import kotlinx.html.unsafe

fun DIV.consentHeader(title: String) {
    div(classes = "consent-header-container") {
        span(classes = "consent-header") { +title }
    }
}

fun DIV.consentImage(imageUrl: String) {
    div(classes = "consent-image-container") {
        img(classes = "consent-image") {
            src = imageUrl
        }
    }
}

fun DIV.consentContainer(page: ConsentPage, block: DIV.(page: ConsentPage) -> Unit) {
    div(classes = "consent-page carousel-item") {
        id = page.pageId
        attributes["data-slide-id"] = page.pageId
        if (page is ConsentPage.ReviewPage) {
            attributes["data-review-consent"] = "true"
        }
        div(classes = "consent-page-container") {
            block(page)
        }
    }
}

fun DIV.wrapInUnsafeParagraph(text: String) {
    p { unsafe { +text } }
}

fun DIV.buttons(page: ConsentPage, block: DIV.(page: ConsentPage) -> Unit) {
    div(classes = "consent-content-buttons") {
        block(page)
    }
}

fun DIV.backButton(page: Backable, block: A.(page: Backable) -> Unit) {
    a(href = "#${page.previousPage}") {
        classes = setOf("carousel-transition", "consent-button", "back-button")
        attributes["data-load-slide"] = page.previousPage
        block(page)
    }
}

fun DIV.nextButton(page: Nextable, block: A.(page: Nextable) -> Unit) {
    a(href = "#${page.nextPage}") {
        classes = setOf("carousel-transition", "consent-button", "next-button")
        attributes["data-load-slide"] = page.nextPage
        block(page)
    }
}

fun DIV.optionToggle(page: ConsentPage.OptionPage, valueString: String, idString: String) {
    radioInput {
        id = idString
        classes = setOf("consent-toggle")
        value = valueString
        attributes["data-question-id"] = "${page.questionId}"
        attributes["data-required"] = "${page.required}"
        name = "question-${page.questionId}"
    }
}

fun DIV.optionLabel(htmlForStr: String, classStr: String, content: String) {
    label {
        classes = setOf(classStr, "container-shadow")
        this.htmlFor = htmlForStr
        div(classes = "select-dots")
        div(classes = "text") {
            unsafe { +content }
        }
    }
}

fun DIV.reviewOptionToggle(questionId: Int, valueString: String, idString: String) {
    radioInput {
        id = idString
        classes = setOf("review-consent-toggle")
        value = valueString
        attributes["data-question-id"] = "$questionId"
        name = "review-question-$questionId"
        disabled = true
    }
}

fun DIV.reviewOptionLabel(htmlForStr: String, classStr: String, content: String) {
    label {
        classes = setOf(classStr)
        this.htmlFor = htmlForStr
        div(classes = "select-dots")
        div(classes = "text") {
            unsafe { +content }
        }
    }
}
