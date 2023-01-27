package com.healthmetrix.dynamicconsent.consent.controllers

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import com.healthmetrix.dynamicconsent.commons.DefaultApiResponse
import com.healthmetrix.dynamicconsent.commons.asEntity
import com.healthmetrix.dynamicconsent.commons.logger
import com.healthmetrix.dynamicconsent.consent.ConsentOption
import com.healthmetrix.dynamicconsent.consent.config.DOCUMENT_API_TAG
import com.healthmetrix.dynamicconsent.consent.usecases.GeneratePdfUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@SecurityRequirement(name = "UserToken")
@Tag(name = DOCUMENT_API_TAG)
class DocumentController(
    private val generatePdfUseCase: GeneratePdfUseCase,
) {
    @PostMapping(
        path = ["/api/v1/consents/{consentId}/documents"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(summary = "Generates a non-sensitive document based on user-selected options and a consent template")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Document created successfully",
                content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid options",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = InvalidDocumentResponse::class),
                    ),
                ],
            ),
        ],
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun generateDocument(
        @RequestBody
        generateDocumentRequestBody: GenerateDocumentRequestBody,
        @Parameter(
            description = "The consent ID of the template to generate with the provided options",
        )
        @PathVariable
        consentId: String,
    ): ResponseEntity<ByteArray> = generatePdfUseCase(consentId, generateDocumentRequestBody.options)
        .map { doc ->
            ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(doc)
        }
        .getOrElse { ex ->
            logger.error("Error generating PDF", ex)
            throw InvalidDocumentException(ex.message ?: "Invalid document")
        }

    @ExceptionHandler(InvalidDocumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidDocumentException(ex: InvalidDocumentException): ResponseEntity<InvalidDocumentResponse> {
        logger.error(ex.message)
        return InvalidDocumentResponse(ex.message ?: "Invalid document").asEntity()
    }

    class InvalidDocumentException(message: String) : Exception(message)
    data class GenerateDocumentRequestBody(val options: List<ConsentOption>)
    data class InvalidDocumentResponse(val message: String) : DefaultApiResponse(HttpStatus.BAD_REQUEST, false)
}
