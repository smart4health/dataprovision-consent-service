package com.healthmetrix.dynamicconsent.commons.pdf

import java.awt.image.BufferedImage

data class VisibleSignatureOptions(
    val signature: BufferedImage,
    val pageNum: Int,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
)
