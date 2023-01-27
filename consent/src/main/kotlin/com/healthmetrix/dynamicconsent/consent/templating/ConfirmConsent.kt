package com.healthmetrix.dynamicconsent.consent.templating

data class ConfirmConsent(
    val title: String,
    val image: String,
    val content: String,
    val navigation: ConfirmConsentNavigation = ConfirmConsentNavigation(),
)

data class ConfirmConsentNavigation(
    val next: String? = null,
    val back: String? = null,
)
