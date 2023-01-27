package com.healthmetrix.dynamicconsent.commons

import org.json.JSONArray
import org.json.JSONObject

/**
 * allows for things such as:
 * array {
 *  json {
 *      "key" to "value"
 *      "otherkey" to json {
 *          "nested" to "objects"
 *      }
 *  }
 * }
 */

fun json(builder: JsonObjectBuilder.() -> Unit) = with(
    JsonObjectBuilder(),
) {
    builder()
    json
}

fun jsonString(builder: JsonObjectBuilder.() -> Unit) = with(JsonObjectBuilder()) {
    builder()
    json.toString()
}

fun array(builder: JsonArrayBuilder.() -> Unit) = with(
    JsonArrayBuilder(),
) {
    builder()
    json
}

class JsonObjectBuilder {
    val json = JSONObject()

    infix fun <T> String.to(value: T) {
        json.put(this, value)
    }

    fun json(builder: JsonObjectBuilder.() -> Unit) =
        com.healthmetrix.dynamicconsent.commons.json(builder)
}

class JsonArrayBuilder {
    val json = JSONArray()

    fun json(builder: JsonObjectBuilder.() -> Unit) {
        json.put(com.healthmetrix.dynamicconsent.commons.json(builder))
    }

    operator fun Any.unaryPlus() {
        json.put(this)
    }
}
