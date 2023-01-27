package com.healthmetrix.dynamicconsent.signing

data class ConsentSigningTemplate(
    val consent: Consent,
    val signing: Signing,
)

data class Consent(
    val version: String,
    val author: String,
)

data class Signing(
    val title: String,
    val review: String,
    val summary: String?,
    val signature: Signature,
    val firstName: Name,
    val lastName: Name,
    val email: Email,
    val conditions: List<Condition>,
    val submit: String,
    val cancel: Cancel,
)

data class Signature(
    val clear: String,
    val errors: SignatureErrors,
)

data class SignatureErrors(val required: String)

data class Name(val label: String)
data class Email(val label: String, val errors: EmailErrors)

data class EmailErrors(val required: String)
data class Condition(val text: String, val required: Boolean)

data class Cancel(
    val button: String,
    val confirmHeader: String,
    val confirmContent: String,
    val confirmCancel: String,
    val rejectCancel: String,
)
