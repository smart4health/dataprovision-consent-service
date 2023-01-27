package com.healthmetrix.dynamicconsent.signing.views

import kotlinx.html.DIV
import kotlinx.html.p
import kotlinx.html.unsafe

fun DIV.wrapInUnsafeParagraph(text: String) {
    p { unsafe { +text } }
}
