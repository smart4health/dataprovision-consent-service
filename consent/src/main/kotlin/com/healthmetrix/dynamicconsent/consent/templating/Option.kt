package com.healthmetrix.dynamicconsent.consent.templating

data class Option(
    val title: String,
    val summary: String,
    val description: String,
    val required: Boolean,
    val accept: String = "Yes",
    val reject: String = "No",
    val navigation: OptionNavigation = OptionNavigation(),
    val popover: String = "To continue, please choose option YES or NO",
    val requiredPopup: RequiredPopup?,
)

class RequiredPopup(
    val title: String,
    val text: String,
    val confirm: String,
    val cancel: String,
)

data class OptionNavigation(
    val next: String? = null,
    val back: String? = null,
    val learnMore: String? = null,
    val close: String? = null,
)
