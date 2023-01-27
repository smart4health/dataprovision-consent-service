package com.healthmetrix.dynamicconsent.consent

data class CancelUrl(private val url: String) {
    val quiz = "$url?reason=quiz"
    val eligibility = "$url?reason=eligibility"
}
