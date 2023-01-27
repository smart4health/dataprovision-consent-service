package com.healthmetrix.dynamicconsent.commons

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import java.util.Base64
import com.github.michaelbull.result.runCatching as catch

private const val DELIMITER = "-"

fun String.decodeBase64(): Result<ByteArray, Throwable> = catch {
    Base64.getDecoder().decode(this)
}

fun String.decodeBase64String(): Result<String, Throwable> = decodeBase64().flatMap {
    catch {
        it.toString(Charsets.UTF_8)
    }
}

fun String.asLocalStaticResourcePath() = "/consent-assets".joinPaths(this)

fun String.consentSource() = this.split(DELIMITER).first()

fun String.consent() = this.split(DELIMITER).drop(1).joinToString(DELIMITER)

fun String.joinPaths(vararg paths: String): String = paths.fold(
    this.trimEnd('/'),
) { acc, path ->
    "$acc/${path.trimStart('/')}"
}.toString()

fun ByteArray.encodeBase64(): String = Base64.getEncoder().encodeToString(this)

fun String.encodeBase64(): String = toByteArray().encodeBase64()
