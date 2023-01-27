package com.healthmetrix.dynamicconsent.commons.pdf.strategies

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.healthmetrix.dynamicconsent.commons.orThrow
import com.healthmetrix.dynamicconsent.commons.pdf.ConsentPdfConfig
import com.healthmetrix.dynamicconsent.commons.pdf.VisibleSignatureOptions
import com.healthmetrix.dynamicconsent.commons.pdf.cropWhitespace
import com.healthmetrix.dynamicconsent.commons.pdf.edit
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.github.michaelbull.result.runCatching as catch

class ConsentSigningStrategy(
    private val consentPdfConfig: ConsentPdfConfig,
) : SigningStrategy {
    override val consentId = consentPdfConfig.consent.id

    override fun sign(
        pdf: PDDocument,
        firstName: String,
        familyName: String,
        visualSignature: BufferedImage,
    ): VisibleSignatureOptions {
        return pdf.apply {
            markDateAndName(firstName, familyName)
        }.markSignature(visualSignature).orThrow()
    }

    private fun PDDocument.markName(firstName: String, lastName: String) {
        if (consentPdfConfig.signing.name != null) {
            val font = getPage(consentPdfConfig.signing.name.page).defaultFont
            val name = "$firstName $lastName"

            // getStringWidth produces 1/1000ths of "text space" units, so divide
            // by 1000 to get normal text space units.  text space is converted to
            // user space by multiplying by font width, (verified with crude measurements
            // in the PDFDebugger) so to fill the max space use a bit of algebra:
            //  maxWidth = stringWidthTextSpace * x,
            //  x = maxWidth / stringWidthTextSpace
            // coerceIn then just keeps the font size within bounds, which means
            // short text will not fill the whole space, and really long text will
            // still overflow, but the font sizes will be reasonable
            val fontSize =
                (consentPdfConfig.signing.name.maxWidth / (font.getStringWidth(name) / 1000))
                    .coerceIn(consentPdfConfig.signing.name.minFontSize..consentPdfConfig.signing.name.maxFontSize)

            edit(consentPdfConfig.signing.name.page) {
                beginText()
                setFont(font, fontSize)
                newLineAtOffset(consentPdfConfig.signing.name.x, consentPdfConfig.signing.name.y)
                showText(name)
                endText()
            }
        }
    }

    private fun PDDocument.markSignature(image: BufferedImage): Result<VisibleSignatureOptions, Throwable> {
        val cropped = image.cropWhitespace()

        val scalingRatio = minOf(
            consentPdfConfig.signing.signature.maxWidth / cropped.width,
            consentPdfConfig.signing.signature.maxHeight / cropped.height,
        )

        // only scale the image if it needs to be scaled down
        val targetWidth = if (scalingRatio < 1) cropped.width * scalingRatio else cropped.width.toFloat()
        val targetHeight = if (scalingRatio < 1) cropped.height * scalingRatio else cropped.height.toFloat()

        return catch {
            edit(consentPdfConfig.signing.signature.page) {
                drawImage(
                    LosslessFactory.createFromImage(this@markSignature, cropped),
                    consentPdfConfig.signing.signature.x,
                    consentPdfConfig.signing.signature.y,
                    targetWidth,
                    targetHeight,
                )
            }
        }.map {
            VisibleSignatureOptions(
                cropped,
                consentPdfConfig.signing.signature.page,
                consentPdfConfig.signing.signature.x,
                consentPdfConfig.signing.signature.y,
                targetWidth,
                targetHeight,
            )
        }
    }

    private fun PDDocument.markDate(now: LocalDateTime = LocalDateTime.now()) {
        if (consentPdfConfig.signing.date != null) {
            edit(consentPdfConfig.signing.date.page) {
                val font = getPage(consentPdfConfig.signing.date.page).defaultFont
                beginText()
                setFont(
                    font,
                    consentPdfConfig.signing.date.fontSize,
                )
                newLineAtOffset(
                    consentPdfConfig.signing.date.x,
                    consentPdfConfig.signing.date.y,
                )

                val formatter = DateTimeFormatter.ofPattern(consentPdfConfig.signing.date.format)
                showText(now.format(formatter))
                endText()
            }
        }
    }

    private fun PDDocument.markDateAndName(
        firstName: String,
        lastName: String,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        if (consentPdfConfig.signing.nameAndDate != null) {
            val date = DateTimeFormatter.ofPattern(consentPdfConfig.signing.nameAndDate.dateFormat).let(now::format)
            val name = "$date, $firstName $lastName"

            val font = getPage(consentPdfConfig.signing.nameAndDate.page).defaultFont
            val fontSize =
                (consentPdfConfig.signing.nameAndDate.maxWidth / (font.getStringWidth(name) / 1000))
                    .coerceIn(12F..consentPdfConfig.signing.nameAndDate.fontSize)

            edit(consentPdfConfig.signing.nameAndDate.page) {
                beginText()
                setFont(font, fontSize)
                newLineAtOffset(consentPdfConfig.signing.nameAndDate.x, consentPdfConfig.signing.nameAndDate.y)
                showText(name)
                endText()
            }
        } else if (consentPdfConfig.signing.date != null && consentPdfConfig.signing.name != null) {
            markDate()
            markName(firstName, lastName)
        }
    }

    /**
     * Some PDFs do not contain any outside dependencies, including fonts (PDF/A, for example). In these scenarios, we need
     * to loop through the available fonts of the PDF and use what's available. This is configurable from the consent
     * configuration YAML.
     */
    private val PDPage.defaultFont: PDFont
        get() {
            if (!consentPdfConfig.fonts.useEmbedded) {
                return consentPdfConfig.fonts.defaultFamily
            }
            val preEmbeddedFont = resources.fontNames.find { fontName ->
                resources.getFont(fontName).name.equals(consentPdfConfig.fonts.defaultFamily.name, ignoreCase = true)
            }
            return preEmbeddedFont?.let(resources::getFont) ?: consentPdfConfig.fonts.defaultFamily
        }
}
