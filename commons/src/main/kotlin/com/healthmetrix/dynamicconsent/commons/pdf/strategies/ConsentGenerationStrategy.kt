package com.healthmetrix.dynamicconsent.commons.pdf.strategies

import com.healthmetrix.dynamicconsent.commons.pdf.ConsentPdfConfig
import com.healthmetrix.dynamicconsent.commons.pdf.asPDDocument
import com.healthmetrix.dynamicconsent.commons.pdf.edit
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.common.PDRectangle
import java.awt.Color
import java.io.InputStream

class ConsentGenerationStrategy(
    private val sourceResource: () -> InputStream,
    private val consentPdfConfig: ConsentPdfConfig,
) : GenerationStrategy {
    override val consentId = consentPdfConfig.consent.id

    override fun generate(options: Options): PDDocument {
        val pdf = sourceResource().asPDDocument()

        consentPdfConfig.options?.inputs?.forEachIndexed { index, input ->
            options[index]?.let { consented ->
                pdf.markQuestion(
                    input.page,
                    OptionCoordinates(
                        yes = PDRectangle(
                            input.yes.x,
                            input.yes.y,
                            consentPdfConfig.options.width,
                            consentPdfConfig.options.height,
                        ),
                        no = PDRectangle(
                            input.no.x,
                            input.no.y,
                            consentPdfConfig.options.width,
                            consentPdfConfig.options.height,
                        ),
                        optionId = index,
                    ),
                    consented,
                )
            }
        }

        return pdf
    }

    private fun PDDocument.markQuestion(pageNumber: Int, coordinates: OptionCoordinates, consented: Boolean) {
        edit(pageNumber) {
            val checkbox = if (consented) coordinates.yes else coordinates.no
            moveTo(checkbox.lowerLeftX, checkbox.lowerLeftY)
            lineTo(checkbox.upperRightX, checkbox.upperRightY)
            setStrokingColor(Color.BLACK)
            stroke()

            moveTo(checkbox.lowerLeftX, checkbox.lowerLeftY + checkbox.height)
            lineTo(checkbox.upperRightX, checkbox.upperRightY - checkbox.height)
            setStrokingColor(Color.BLACK)
            stroke()
        }
    }
}

data class OptionCoordinates(val yes: PDRectangle, val no: PDRectangle, val optionId: Int)
