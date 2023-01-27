package com.healthmetrix.dynamicconsent.signing.controllers

import com.healthmetrix.dynamicconsent.commons.DefaultApiResponse
import com.healthmetrix.dynamicconsent.commons.asEntity
import com.healthmetrix.dynamicconsent.commons.orThrow
import com.healthmetrix.dynamicconsent.signing.config.SIGNATURE_API_TAG
import com.healthmetrix.dynamicconsent.signing.usecases.ConsentInfo
import com.healthmetrix.dynamicconsent.signing.usecases.GetConsentInfoPerUserUseCase
import com.healthmetrix.dynamicconsent.signing.verifyjwt.JwtInfoExtractor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
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
class SignatureStatusController(
    private val jwtInfoExtractor: JwtInfoExtractor,
    private val getConsentInfoPerUserUseCase: GetConsentInfoPerUserUseCase,
) {
    @GetMapping(
        path = ["/api/v2/signatures/status"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(
        summary = "Generate Status about all consented and withdrawn consents for a token, " +
            "counting only the most recent/active per consentId",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Response contains consentInfo lists, one item per consentId",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(
                            implementation = SignatureStatusResponse.Success::class,
                            hidden = true,
                        ),
                    ),
                ],

            ),
        ],
    )
    fun getStatus(
        @RequestHeader("Authorization")
        authHeader: String,
    ): ResponseEntity<SignatureStatusResponse> {
        val (consented, withdrawn) = getConsentInfoPerUserUseCase(authHeader.externalRefId())
        return SignatureStatusResponse.Success(consented, withdrawn).asEntity()
    }

    private fun String.removeBearer() = removePrefix("Bearer ")

    private fun String.externalRefId() = removeBearer()
        .let(jwtInfoExtractor::invoke)
        .orThrow()
        .externalRefId
}

sealed class SignatureStatusResponse(
    httpStatus: HttpStatus,
    hasBody: Boolean,
) : DefaultApiResponse(httpStatus, hasBody) {
    data class Success(val consented: List<ConsentInfo>, val withdrawn: List<ConsentInfo>) :
        SignatureStatusResponse(HttpStatus.OK, true)
}
