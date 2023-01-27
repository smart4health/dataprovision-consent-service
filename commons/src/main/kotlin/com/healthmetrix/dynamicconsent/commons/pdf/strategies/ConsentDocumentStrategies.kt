package com.healthmetrix.dynamicconsent.commons.pdf.strategies

import com.healthmetrix.dynamicconsent.commons.pdf.VisibleSignatureOptions
import org.apache.pdfbox.pdmodel.PDDocument
import java.awt.image.BufferedImage

/**
 * Controls document generation
 */
interface GenerationStrategy {

    /**
     * Consent ID this strategy is valid for
     *
     * Values should be unique across all generation strategies
     */
    val consentId: String

    /**
     * Create a PDF marked with the given options
     */
    fun generate(options: Options): PDDocument
}

/**
 * Controls document signing
 */
interface SigningStrategy {

    /**
     * Consent ID this strategy is valid for
     *
     * Values should be unique across all signing strategies
     */
    val consentId: String

    /**
     * Visually sign a document with the given name and signature image
     *
     * If a strategy wishes to use visual *cryptographic* signatures as per
     * the PDF spec, return a non null VisibleSignatureOptions object
     */
    fun sign(
        pdf: PDDocument,
        firstName: String,
        familyName: String,
        visualSignature: BufferedImage,
    ): VisibleSignatureOptions?
}

typealias Options = Map<Int, Boolean> // question number, has consented
