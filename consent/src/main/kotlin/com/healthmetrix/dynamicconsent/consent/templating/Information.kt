package com.healthmetrix.dynamicconsent.consent.templating

data class Information(
    val title: String,
    val image: String,
    val summary: String,
    val description: String,
    val navigation: InformationNavigation = InformationNavigation(),
)

data class InformationNavigation(
    val next: String? = null,
    val back: String? = null,
    val learnMore: String? = null,
    val close: String? = null,
)
