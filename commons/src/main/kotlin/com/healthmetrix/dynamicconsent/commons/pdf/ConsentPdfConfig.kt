package com.healthmetrix.dynamicconsent.commons.pdf

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class ConsentOverviewConfig(
    val version: String,
    val author: String,
    val id: String,
    val lang: String,
)

data class ConsentFontsConfig
@JsonCreator(mode = PROPERTIES)
constructor(
    private val default: String,
    val useEmbedded: Boolean = false,
) {
    @JsonIgnore
    val defaultFamily: PDType1Font = when (default.lowercase(Locale.getDefault())) {
        "helvetica" -> PDType1Font.HELVETICA
        else -> throw Error("Font $default not supported")
    }
}

data class OptionCoordinateConfig(
    val x: Float,
    val y: Float,
)

data class ConsentInputConfig(
    val page: Int,
    val yes: OptionCoordinateConfig,
    val no: OptionCoordinateConfig,
)

data class ConsentOptionsConfig(
    val width: Float,
    val height: Float,
    val inputs: List<ConsentInputConfig>,
)

data class SigningNameConfig(
    val page: Int,
    val x: Float,
    val y: Float,
    val maxWidth: Float,
    private val fontSize: Float,
) {
    @JsonIgnore
    val minFontSize = min(12F, fontSize)

    @JsonIgnore
    val maxFontSize = max(minFontSize, fontSize)
}

data class SigningDateConfig(
    val page: Int,
    val x: Float,
    val y: Float,
    val fontSize: Float,
    val format: String,
)

data class SigningSignatureConfig(
    val page: Int,
    val x: Float,
    val y: Float,
    val maxWidth: Float = 200F,
    val maxHeight: Float = 80F,
)

data class SigningNameAndDateConfig(
    val page: Int,
    val x: Float,
    val y: Float,
    val maxWidth: Float,
    val fontSize: Float,
    val dateFormat: String,
)

data class ConsentSigningConfig(
    val name: SigningNameConfig?,
    val date: SigningDateConfig?,
    val nameAndDate: SigningNameAndDateConfig?,
    val signature: SigningSignatureConfig,
)

data class ConsentPdfConfig(
    val consent: ConsentOverviewConfig,
    val fonts: ConsentFontsConfig,
    val options: ConsentOptionsConfig?,
    val signing: ConsentSigningConfig,
)
