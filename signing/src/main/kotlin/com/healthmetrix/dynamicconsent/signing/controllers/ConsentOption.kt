package com.healthmetrix.dynamicconsent.signing.controllers

import io.swagger.v3.oas.annotations.media.Schema

data class ConsentOption(
    @Schema(
        description = "An identifier of the option the user is accepting or denying consent for. Implemented as an index of an array",
        required = true,
        example = "2",
    )
    val optionId: Int,
    @Schema(
        description = "User-selected choice",
        required = true,
        example = "true",
    )
    val consented: Boolean,
)
