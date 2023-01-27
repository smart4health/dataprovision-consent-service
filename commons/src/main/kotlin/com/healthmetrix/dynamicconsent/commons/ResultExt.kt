package com.healthmetrix.dynamicconsent.commons

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

fun <V, E : Throwable> Result<V, E>.orThrow(): V {
    return when (this) {
        is Ok -> value
        is Err -> throw error
    }
}
