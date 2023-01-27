package com.healthmetrix.dynamicconsent.signing.controllers

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.merge
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.dynamicconsent.commons.DefaultApiResponse
import com.healthmetrix.dynamicconsent.commons.asEntity
import com.healthmetrix.dynamicconsent.commons.logger
import com.healthmetrix.dynamicconsent.commons.orThrow
import com.healthmetrix.dynamicconsent.signing.DocumentID
import com.healthmetrix.dynamicconsent.signing.SignatureToken
import com.healthmetrix.dynamicconsent.signing.SignatureTokenViewModel
import com.healthmetrix.dynamicconsent.signing.config.SIGNATURE_API_TAG
import com.healthmetrix.dynamicconsent.signing.usecases.CacheDocumentError
import com.healthmetrix.dynamicconsent.signing.usecases.CacheDocumentUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.DecryptSignatureTokenUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.EncryptSignatureTokenUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.GetLeastRecentlySignedPdfUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.SelectSigningTemplateUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.SignPdfUseCase
import com.healthmetrix.dynamicconsent.signing.usecases.WithdrawSignatureError
import com.healthmetrix.dynamicconsent.signing.usecases.WithdrawSignatureUseCase
import com.healthmetrix.dynamicconsent.signing.verifyjwt.JwtInfoExtractor
import com.healthmetrix.dynamicconsent.signing.views.SignDocumentView
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized action",
            headers = [Header(name = "WWW-Authenticate", schema = Schema(type = "string"))],
            content = [Content()],
        ),
    ],
)
@SecurityRequirement(name = "UserToken")
@Tag(name = SIGNATURE_API_TAG)
class SignatureController(
    private val jwtInfoExtractor: JwtInfoExtractor,
    private val cacheDocumentUseCase: CacheDocumentUseCase,
    private val signDocumentView: SignDocumentView,
    private val signPdfUseCase: SignPdfUseCase,
    private val getLeastRecentlySignedPdfUseCase: GetLeastRecentlySignedPdfUseCase,
    private val encryptSignatureTokenUseCase: EncryptSignatureTokenUseCase,
    private val decryptSignatureTokenUseCase: DecryptSignatureTokenUseCase,
    private val selectSigningTemplateUseCase: SelectSigningTemplateUseCase,
    private val withdrawSignatureUseCase: WithdrawSignatureUseCase,
) {

    @PostMapping(
        path = ["/api/v1/signatures"],
        consumes = [MediaType.APPLICATION_PDF_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(summary = "Temporarily stores a document as a pseudo-cache for a future signature")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "PDF successfully stored",
                content = [
                    Content(schema = Schema(implementation = CreateSignatureResponse.Success::class)),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Error Creating Signature Token",
                content = [
                    Content(schema = Schema(implementation = CreateSignatureResponse.FailedTokenGeneration::class)),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Error storing document",
                content = [
                    Content(schema = Schema(implementation = CreateSignatureResponse.FailedUpload::class)),
                ],
            ),
        ],
    )
    fun createSignature(
        @RequestBody
        document: ByteArray,
        @RequestHeader("Authorization")
        authHeader: String,
        @RequestHeader("X-Hmx-Success-Redirect-Url")
        @Parameter(description = "URL to redirect to when the document is signed")
        successRedirectUrl: String,
        @RequestHeader("X-Hmx-Cancel-Redirect-Url")
        @Parameter(
            required = false,
            description = "URL to redirect to when the signing flow is cancelled. A query parameter for the reason will be appended to the URL",
        )
        cancelRedirectUrl: String? = null,
        @RequestHeader("X-Hmx-Review-Consent-Url")
        @Parameter(
            required = false,
            description = "URL to go back to review the consent",
        )
        reviewConsentUrl: String? = null,
        @RequestHeader("X-Hmx-Consent-Id")
        @Parameter(description = "The type of consent document that is being signed")
        consentId: String,
        @RequestHeader("X-Hmx-Platform")
        @Parameter(
            description = "The platform the user is using. Helpful to render platform-specific styles.",
            required = false,
            schema = Schema(implementation = SignaturePlatform::class),
        )
        platform: String? = null,
    ): ResponseEntity<CreateSignatureResponse> = binding<CreateSignatureResponse, CreateSignatureResponse> {
        val externalRefId = authHeader.externalRefId()
        val documentId = cacheDocumentUseCase(externalRefId, document, consentId)
            .onFailure { logger.error("Error storing document", it) }
            .mapError { error ->
                when (error) {
                    is CacheDocumentError.AlreadyConsentedException -> CreateSignatureResponse.AlreadyConsented
                    else -> CreateSignatureResponse.FailedUpload
                }
            }
            .bind()

        val signaturePlatform = SignaturePlatform.from(platform)
            .onFailure { logger.error("Platform $platform not supported") }
            .mapError { CreateSignatureResponse.FailedUpload }
            .bind()

        val signatureToken = SignatureToken(
            successRedirectUrl = successRedirectUrl,
            cancelRedirectUrl = cancelRedirectUrl,
            authToken = authHeader.removePrefix("Bearer "),
            reviewConsentUrl = reviewConsentUrl,
            consentId = consentId,
            platform = signaturePlatform?.platform,
        ).let(encryptSignatureTokenUseCase::invoke)
            .onFailure { logger.error("Error creating signature token", it) }
            .mapError { CreateSignatureResponse.FailedTokenGeneration }
            .bind()

        CreateSignatureResponse.Success(documentId, signatureToken)
    }.merge().asEntity()

    @GetMapping(
        path = ["/api/v1/signatures/{documentId}"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(summary = "Gets a signed PDF")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Signature found",
                content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)],
            ),
            ApiResponse(
                responseCode = "404",
                description = "No Signature Found",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = GetSignatureResponse.NotFound::class, hidden = true),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal Error",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = GetSignatureResponse.InternalError::class, hidden = true),
                    ),
                ],
            ),
        ],
    )
    @Deprecated(
        "Use new endpoint not using documentId, but consentId + externalRefId",
        replaceWith = ReplaceWith("/api/v2/signatures"),
    )
    fun getSignature(
        @Parameter(description = "Document ID")
        @PathVariable
        documentId: String,
        @RequestHeader("Authorization")
        authHeader: String,
    ): ResponseEntity<ByteArray> {
        val externalRefId = authHeader.externalRefId()

        return getLeastRecentlySignedPdfUseCase(externalRefId, DocumentID.fromString(documentId))
            .map { document ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(document)
            }
            .getOrElse { ex -> throw ex }
    }

    @GetMapping(
        path = ["/api/v2/signatures"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(summary = "Gets a signed PDF")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Signature found",
                content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)],

            ),
            ApiResponse(
                responseCode = "404",
                description = "No Signature Found",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = GetSignatureResponse.NotFound::class, hidden = true),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal Error",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = GetSignatureResponse.InternalError::class, hidden = true),
                    ),
                ],
            ),
        ],
    )
    fun getSignatureV2(
        @Parameter(description = "Consent ID", example = "smart4health-research-consent-en", required = true)
        @RequestParam
        consentId: String,
        @RequestHeader("Authorization")
        authHeader: String,
    ): ResponseEntity<ByteArray> {
        val externalRefId = authHeader.externalRefId()

        return getLeastRecentlySignedPdfUseCase(externalRefId, consentId)
            .map { document ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(document)
            }
            .getOrElse { ex -> throw ex }
    }

    @GetMapping("/signatures/{documentId}/sign")
    @Operation(description = "Loads a form for the user to sign a specific document")
    @ResponseBody
    fun newSignature(
        @Parameter(description = "Encrypted base64-encoded token", schema = Schema(type = "string"))
        @RequestParam
        token: String,
        @Parameter(description = "Document ID")
        @PathVariable
        documentId: String,
    ): StringBuilder = binding<StringBuilder, Throwable> {
        val signatureToken = decryptSignatureTokenUseCase(token).bind()
        val signingTemplate = selectSigningTemplateUseCase(signatureToken.consentId).bind()
        val tokenModel = SignatureTokenViewModel(
            successRedirectUrl = signatureToken.successRedirectUrl,
            cancelRedirectUrl = signatureToken.cancelRedirectUrl,
            reviewConsentUrl = signatureToken.reviewConsentUrl,
            authToken = signatureToken.authToken,
            encryptedToken = token,
            platform = signatureToken.platform,
            consentId = signatureToken.consentId,
        )
        signDocumentView.build(
            signingTemplate = signingTemplate,
            tokenViewModel = tokenModel,
            documentId = documentId,
        )
    }.orThrow()

    @PutMapping("/api/v1/signatures/{documentId}/sign")
    @Operation(description = "Updates a document PDF with personal information and signs it")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "301",
                description = "Document Signed",
                content = [
                    Content(
                        schema = Schema(
                            implementation = SignDocumentResponse.Success::class,
                            hidden = true,
                        ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Document not found",
                content = [
                    Content(
                        schema = Schema(
                            implementation = SignDocumentResponse.NotFound::class,
                            hidden = true,
                        ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Error storing document",
                content = [
                    Content(schema = Schema(implementation = SignDocumentResponse.InternalError::class)),
                ],
            ),
        ],
    )
    @ResponseBody
    fun signDocument(
        @Parameter(description = "The document ID of the document to sign")
        @PathVariable
        documentId: String,
        @RequestBody
        signDocumentRequestBody: SignDocumentRequestBody,
    ): ResponseEntity<SignDocumentResponse> {
        return binding<SignDocumentResponse, Throwable> {
            val signatureToken = decryptSignatureTokenUseCase(signDocumentRequestBody.token)
                .bind()

            val jwtInfo = signatureToken.authToken
                .let(jwtInfoExtractor::invoke)
                .orThrow()

            signPdfUseCase(
                documentId = documentId,
                externalRefId = jwtInfo.externalRefId,
                firstName = signDocumentRequestBody.firstName,
                lastName = signDocumentRequestBody.lastName,
                signature = signDocumentRequestBody.signature,
                metadata = jwtInfo.metadata,
                email = signDocumentRequestBody.email,
            ).bind()

            SignDocumentResponse.Success(successRedirectUrl = signatureToken.successRedirectUrl)
        }.onFailure { ex ->
            logger.error("Error storing signed pdf", ex)
        }.mapError {
            SignDocumentResponse.InternalError
        }.merge().asEntity()
    }

    @PostMapping(
        path = ["/api/v1/signatures/{documentId}/withdraw"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(summary = "Withdraws a given consent and stores the time at which the consent was withdrawn")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Consent withdrawn",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(
                            implementation = WithdrawSignatureResponse.Success::class,
                            hidden = true,
                        ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Signature already withdrawn",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = WithdrawSignatureResponse.AlreadyWithdrawn::class),
                    ),
                ],
            ),
        ],
    )
    @Deprecated(
        "Use new endpoint not using documentId, but consentId + externalRefId",
        replaceWith = ReplaceWith("/api/v2/signatures"),
    )
    fun withdrawSignature(
        @Parameter(description = "Document ID")
        @PathVariable
        documentId: String,
        @RequestHeader("Authorization")
        authHeader: String,
    ): ResponseEntity<WithdrawSignatureResponse> {
        val externalRefId = authHeader.externalRefId()
        return withdrawSignatureUseCase(documentId = documentId)
            .onFailure { error ->
                logger.info("Failed to withdraw signature for consent flow id $externalRefId; error ${error::class.simpleName}")
            }.mapError { error ->
                when (error) {
                    is WithdrawSignatureError.SignatureAlreadyWithdrawn -> WithdrawSignatureResponse.AlreadyWithdrawn
                    is WithdrawSignatureError.SignatureNotFound -> WithdrawSignatureResponse.NotFound
                    WithdrawSignatureError.UpdateWithdrawnAtFailed -> WithdrawSignatureResponse.Failure
                }
            }.map { WithdrawSignatureResponse.Success }.merge().asEntity()
    }

    @DeleteMapping(
        path = ["/api/v2/signatures"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(summary = "Withdraws a given consent and stores the time at which the consent was withdrawn")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Consent withdrawn",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(
                            implementation = WithdrawSignatureResponse.Success::class,
                            hidden = true,
                        ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Request failure",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = WithdrawSignatureResponse.Failure::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Signature already withdrawn",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = WithdrawSignatureResponse.AlreadyWithdrawn::class),
                    ),
                ],
            ),
        ],
    )
    fun withdrawSignatureV2(
        @Parameter(description = "Consent ID", example = "smart4health-research-consent-en", required = true)
        @RequestParam
        consentId: String,
        @RequestHeader("Authorization")
        authHeader: String,
    ): ResponseEntity<WithdrawSignatureResponse> {
        val externalRefId = authHeader.externalRefId()
        return withdrawSignatureUseCase(consentId = consentId, externalRefId = externalRefId)
            .onFailure { error ->
                logger.info("Failed to withdraw signature for consent flow id $externalRefId; error ${error::class.simpleName}")
            }.mapError { error ->
                when (error) {
                    is WithdrawSignatureError.SignatureAlreadyWithdrawn -> WithdrawSignatureResponse.AlreadyWithdrawn
                    is WithdrawSignatureError.SignatureNotFound -> WithdrawSignatureResponse.NotFound
                    WithdrawSignatureError.UpdateWithdrawnAtFailed -> WithdrawSignatureResponse.Failure
                }
            }.map { WithdrawSignatureResponse.Success }.merge().asEntity()
    }

    private fun String.removeBearer() = removePrefix("Bearer ")

    private fun String.externalRefId() = removeBearer()
        .let(jwtInfoExtractor::invoke)
        .orThrow()
        .externalRefId
}

sealed class CreateSignatureResponse(httpStatus: HttpStatus) : DefaultApiResponse(httpStatus) {
    data class Success(
        @Schema(example = "cb442788-55c6-4779-bf03-0dca9fe1ab08")
        val documentId: String,
        @Schema(example = "")
        val token: String,
    ) : CreateSignatureResponse(HttpStatus.CREATED)

    object FailedTokenGeneration : CreateSignatureResponse(HttpStatus.INTERNAL_SERVER_ERROR) {
        @Suppress("MayBeConstant", "unused")
        val message = "Failed to create token"
    }

    object FailedUpload : CreateSignatureResponse(HttpStatus.INTERNAL_SERVER_ERROR) {
        @Suppress("MayBeConstant", "unused")
        val message = "Failed to upload document"
    }

    object AlreadyConsented : CreateSignatureResponse(HttpStatus.CONFLICT) {
        @Suppress("MayBeConstant", "unused")
        val message = "Consent already given with this consentId and externalRefId"
    }
}

sealed class GetSignatureResponse(httpStatus: HttpStatus, hasBody: Boolean = false) :
    DefaultApiResponse(httpStatus, hasBody) {
    object NotFound : GetSignatureResponse(HttpStatus.NOT_FOUND)
    object InternalError : GetSignatureResponse(HttpStatus.INTERNAL_SERVER_ERROR)
}

data class SignDocumentRequestBody(
    @Schema(type = "string", description = "The user's first name")
    val firstName: String,
    @Schema(type = "string", description = "the user's last name")
    val lastName: String,
    @Schema(type = "string", description = "user's email", required = false)
    val email: String? = null,
    @Schema(type = "string", description = "the signature token")
    val token: String,
    @Schema(type = "string", description = "Image of the signature")
    val signature: String,
)

sealed class SignDocumentResponse(httpStatus: HttpStatus, hasBody: Boolean = false, headers: HttpHeaders) :
    DefaultApiResponse(httpStatus, hasBody, headers) {
    data class Success(val successRedirectUrl: String) :
        SignDocumentResponse(HttpStatus.OK, true, HttpHeaders())

    object NotFound : SignDocumentResponse(HttpStatus.NOT_FOUND, false, HttpHeaders())
    object InternalError : SignDocumentResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, HttpHeaders())
}

@Schema(enumAsRef = true, required = false)
enum class SignaturePlatform(val platform: String) {
    ANDROID("android"),
    IOS("ios"),
    ;

    companion object {
        fun from(identifier: String?): Result<SignaturePlatform?, Throwable> {
            return if (identifier == null) {
                Ok(null)
            } else {
                values().find { it.platform.equals(identifier, ignoreCase = true) }
                    .toResultOr { Error("Could not find platform $identifier") }
            }
        }
    }
}

sealed class WithdrawSignatureResponse(
    httpStatus: HttpStatus,
    hasBody: Boolean,
) : DefaultApiResponse(httpStatus, hasBody) {
    object NotFound : WithdrawSignatureResponse(HttpStatus.NOT_FOUND, false)
    object Failure : WithdrawSignatureResponse(HttpStatus.BAD_REQUEST, false)
    object AlreadyWithdrawn : WithdrawSignatureResponse(HttpStatus.CONFLICT, false)
    object Success : WithdrawSignatureResponse(HttpStatus.OK, false)
}
