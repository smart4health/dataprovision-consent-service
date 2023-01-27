package com.healthmetrix.dynamicconsent.consent.views.components

import kotlinx.html.DIV
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.span

fun DIV.consentCheckbox(block: INPUT.() -> Unit = {}) {
    label(classes = "consent-checkbox-label") {
        input(classes = "consent-check", type = InputType.checkBox) {
            block()
        }
        span(classes = "consent-checkmark")
    }
}
