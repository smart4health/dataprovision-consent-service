package com.healthmetrix.dynamicconsent.signing.controllers

import com.healthmetrix.dynamicconsent.commons.logger
import com.healthmetrix.dynamicconsent.signing.verifyjwt.JwtInfoExtractorException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class TokenVerificationAdvice {

    @ExceptionHandler(JwtInfoExtractorException::class)
    fun handleJwtInfoExtractorException(ex: JwtInfoExtractorException): ResponseEntity<String> {
        logger.info("Failed to verify bearer token", ex)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Failed to verify bearer token")
    }
}
