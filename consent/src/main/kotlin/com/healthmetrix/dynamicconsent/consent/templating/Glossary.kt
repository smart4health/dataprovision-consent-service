package com.healthmetrix.dynamicconsent.consent.templating

data class Glossary(
    val closeText: String,
    val items: List<GlossaryItem>,
)

data class GlossaryItem(
    val id: String,
    val text: String,
    val config: Config?,
) {
    data class Config(
        val reviewPageTitle: String?,
    )
}
