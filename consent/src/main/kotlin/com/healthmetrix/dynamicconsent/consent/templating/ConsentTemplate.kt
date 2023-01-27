package com.healthmetrix.dynamicconsent.consent.templating

/**
 * Representation of the raw data parsed from
 * the YAML template, before it has been manipulated
 * or converted
 */
data class ConsentTemplate(
    val consent: Config,
    val defaults: Defaults,
    val welcome: Welcome,
    val eligibility: Eligibility?,
    val information: List<Information>,
    val options: List<Option> = listOf(),
    val quiz: Quiz?,
    val confirm: ConfirmConsent?,
    val review: Review?,
    val glossary: GlossaryTemplate?,
)

data class Config(
    val version: String?,
    val options: Map<String, String> = mapOf(),
    val author: String?,
    val learnMoreDisplayType: LearnMoreDisplayType = LearnMoreDisplayType.PAGE,
)

data class Defaults(
    val navigation: NavigationDefaults,
)

data class NavigationDefaults(
    val back: String,
    val next: String,
    val learnMore: String,
    val answer: String,
    val exit: String,
    val share: String,
    val close: String = "CLOSE",
)

data class GlossaryTemplate(
    val items: List<GlossaryItem>,
    val config: Config?,
) {
    data class Config(
        val reviewPageTitle: String?,
    )
}

enum class LearnMoreDisplayType {
    MODAL, PAGE, NONE
}
