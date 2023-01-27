package com.healthmetrix.dynamicconsent.commons.pdf

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun BufferedImage.cropWhitespace(): BufferedImage {
    val top = (0 until height).asSequence().flatMap { y ->
        (0 until width).asSequence().map { x -> x to y }
    }.firstOrNull { (x, y) ->
        getRGB(x, y) == Color.BLACK.rgb
    }?.second ?: 0

    val bottom = (height - 1 downTo 0).asSequence().flatMap { y ->
        (0 until width).asSequence().map { x -> x to y }
    }.firstOrNull { (x, y) ->
        getRGB(x, y) == Color.BLACK.rgb
    }?.second ?: height - 1

    val left = (0 until width).asSequence().flatMap { x ->
        (0 until height).asSequence().map { y -> x to y }
    }.firstOrNull { (x, y) ->
        getRGB(x, y) == Color.BLACK.rgb
    }?.first ?: 0

    val right = (width - 1 downTo 0).asSequence().flatMap { x ->
        (0 until height).asSequence().map { y -> x to y }
    }.firstOrNull { (x, y) ->
        getRGB(x, y) == Color.BLACK.rgb
    }?.first ?: width - 1

    // all these are inclusive, so to get the width/height, we need to add 1
    return getSubimage(left, top, right - left + 1, bottom - top + 1)
}

fun BufferedImage.pngBytes(): ByteArray = ByteArrayOutputStream().also {
    ImageIO.write(this, "png", it)
}.toByteArray()
