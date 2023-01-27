package com.healthmetrix.dynamicconsent.signing

data class SignatureToken(
    val successRedirectUrl: String,
    val authToken: String,
    val cancelRedirectUrl: String? = null,
    val reviewConsentUrl: String? = null,
    val consentId: String,
    val platform: String? = null,
)

data class SignatureTokenViewModel(
    val successRedirectUrl: String,
    val authToken: String,
    val cancelRedirectUrl: String? = null,
    val reviewConsentUrl: String? = null,
    val encryptedToken: String,
    val consentId: String,
    val platform: String? = null,
)
