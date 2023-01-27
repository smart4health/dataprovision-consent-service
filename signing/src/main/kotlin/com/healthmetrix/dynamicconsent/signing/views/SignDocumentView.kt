package com.healthmetrix.dynamicconsent.signing.views

import com.healthmetrix.dynamicconsent.commons.HTMLBuilder
import com.healthmetrix.dynamicconsent.commons.asLocalStaticResourcePath
import com.healthmetrix.dynamicconsent.signing.ConsentSigningTemplate
import com.healthmetrix.dynamicconsent.signing.SignatureTokenViewModel
import com.healthmetrix.dynamicconsent.signing.SigningRepository
import kotlinx.html.ButtonType
import kotlinx.html.FORM
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.canvas
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.fieldSet
import kotlinx.html.hr
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.putForm
import kotlinx.html.span
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SignDocumentView(
    @Value("\${signing.backend-url}")
    private val backendUrl: String,
    private val signingRepository: SigningRepository,
) {
    fun build(
        signingTemplate: ConsentSigningTemplate,
        tokenViewModel: SignatureTokenViewModel,
        documentId: String,
    ): StringBuilder = HTMLBuilder().apply {
        head {
            title = "Sign Consent"
            stylesheets {
                add("/styles/signature.css".asLocalStaticResourcePath())

                add(signingRepository.staticPath(tokenViewModel.consentId, "/styles/styles.css"))
                if (tokenViewModel.platform != null) {
                    add(
                        signingRepository.staticPath(
                            tokenViewModel.consentId,
                            "/styles/styles-${tokenViewModel.platform}.css",
                        ),
                    )
                }
            }
            scripts { add("/signing-bundle.js".asLocalStaticResourcePath()) }
        }
    }.build().buildView {
        div(classes = "signature-container") {
            if (tokenViewModel.reviewConsentUrl != null) {
                div(classes = "review-consent-container") {
                    a(href = tokenViewModel.reviewConsentUrl) {
                        classes += setOf("button", "cta-main")
                        id = "review-consent-button"
                        +signingTemplate.signing.review
                    }
                }
            }
            putForm {
                action = "$backendUrl/api/v1/signatures/$documentId/sign"
                id = "signature-form"
                attributes["data-signature-token"] = tokenViewModel.encryptedToken
                div(classes = "form-header largest-font header") {
                    span { +signingTemplate.signing.title }
                }
                signingTemplate.signing.summary?.let {
                    div(classes = "form-section form-summary-container") {
                        wrapInUnsafeParagraph(signingTemplate.signing.summary)
                    }
                }
                signatureTextInput(id = "first-name", label = signingTemplate.signing.firstName.label)
                signatureTextInput(id = "last-name", label = signingTemplate.signing.lastName.label)
                signatureTextInput(
                    id = "email",
                    label = signingTemplate.signing.email.label,
                    errorMessage = signingTemplate.signing.email.errors.required,
                    inputType = InputType.email,
                )
                div(classes = "form-section") {
                    id = "canvas-container"
                    canvas {
                        id = "signature-canvas"
                        width = "500"
                    }
                    div(classes = "hidden") {
                        id = "signature-form-error"
                        div {
                            +signingTemplate.signing.signature.errors.required
                        }
                    }
                }
                div {
                    id = "clear-signature-container"
                    button(classes = "link") {
                        id = "clear-signature-button"
                        type = ButtonType.button
                        +signingTemplate.signing.signature.clear
                    }
                }
                div {
                    classes = setOf("confirm-signature-container", "form-section")
                    signingTemplate.signing.conditions.forEachIndexed { index, condition ->
                        p {
                            label {
                                attributes["for"] = "condition-$index"
                                input(classes = "filled-in") {
                                    id = "condition-$index"
                                    type = InputType.checkBox
                                    name = "confirm-signature"
                                    required = condition.required
                                }
                                span {
                                    +condition.text
                                }
                            }
                        }
                    }
                }

                div(classes = "submit-form-container form-section") {
                    if (tokenViewModel.cancelRedirectUrl != null) {
                        button(classes = "medium-font") {
                            attributes["data-cancel-redirect-url"] = tokenViewModel.cancelRedirectUrl
                            id = "cancel-button"
                            type = ButtonType.button
                            +signingTemplate.signing.cancel.button
                        }
                    }
                    button {
                        id = "sign-button"
                        type = ButtonType.submit
                        classes = setOf("medium-font", "button", "cta-main")
                        +signingTemplate.signing.submit
                    }
                }
            }
        }
        div(classes = "modal") {
            id = "confirm-cancel-modal"
            div(classes = "modal-content") {
                p(classes = "cancel-modal-header center-text") { +signingTemplate.signing.cancel.confirmHeader }
                p(classes = "cancel-modal-body center-text") { +signingTemplate.signing.cancel.confirmContent }
                div(classes = "modal-options") {
                    div(classes = "modal-option") {
                        id = "confirm-confirm-cancel-modal"
                        div(classes = "modal-option-text") {
                            +signingTemplate.signing.cancel.confirmCancel
                        }
                    }
                    div(classes = "modal-option") {
                        id = "close-confirm-cancel-modal"
                        div(classes = "modal-option-text") {
                            +signingTemplate.signing.cancel.rejectCancel
                        }
                    }
                }
            }
        }
    }

    fun FORM.signatureTextInput(
        id: String,
        label: String,
        inputType: InputType = InputType.text,
        errorMessage: String? = null,
    ) {
        div(classes = "form-input-section") {
            div {
                classes = setOf("input-field", "signature-text-input-container")
                fieldSet {
                    classes += setOf("material")
                    input {
                        this.id = id
                        type = inputType
                        attributes["aria-labelledby"] = id
                        name = id
                        required = true
                    }
                    hr { }
                    label {
                        attributes["for"] = id
                        +label
                    }
                }
            }
            if (!errorMessage.isNullOrEmpty()) {
                div(classes = "hidden text-error-container") {
                    this.id = "$id-error"
                    +errorMessage
                }
            }
        }
    }
}
