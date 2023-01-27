package com.healthmetrix.dynamicconsent.consent.templating

data class Eligibility(
    val introduction: EligibilityIntroduction,
    val checks: EligibilityChecks,
    val ineligible: EligibilityFailed,
    val completed: EligibilityCompleted,
)

data class EligibilityIntroductionNavigation(
    val next: String? = null,
    val back: String? = null,
)

data class EligibilityIntroduction(
    val title: String,
    val content: String,
    val image: String,
    val navigation: EligibilityIntroductionNavigation = EligibilityIntroductionNavigation(),
)

data class EligibilityFailedNavigation(val next: String? = null)

data class EligibilityFailed(
    val title: String,
    val content: String,
    val image: String,
    val navigation: EligibilityFailedNavigation = EligibilityFailedNavigation(),
)

data class EligibilityChecksNavigation(
    val next: String? = null,
    val back: String? = null,
)

data class EligibilityCheck(
    val check: String,
    val eligible: Boolean,
)

data class EligibilityChecks(
    val title: String,
    val checks: List<EligibilityCheck>,
    val navigation: EligibilityChecksNavigation = EligibilityChecksNavigation(),
)

data class EligibilityCompletedNavigation(val next: String? = null)

data class EligibilityCompleted(
    val title: String,
    val image: String,
    val content: String,
    val navigation: EligibilityCompletedNavigation = EligibilityCompletedNavigation(),
)
