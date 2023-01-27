package com.healthmetrix.dynamicconsent.consent.templating

data class Review(
    val type: ReviewType,
    val source: String,
)

enum class ReviewType { EXTERNAL }
