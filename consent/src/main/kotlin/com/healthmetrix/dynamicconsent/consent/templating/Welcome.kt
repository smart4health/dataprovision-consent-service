package com.healthmetrix.dynamicconsent.consent.templating

data class WelcomeNavigationTexts(
    val next: String? = null,
)

data class Welcome(
    val title: String,
    val image: String,
    val description: String,
    val navigation: WelcomeNavigationTexts = WelcomeNavigationTexts(),
)
