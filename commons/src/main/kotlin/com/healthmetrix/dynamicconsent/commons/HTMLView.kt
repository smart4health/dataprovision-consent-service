package com.healthmetrix.dynamicconsent.commons

import kotlinx.html.BODY
import kotlinx.html.ScriptType
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.stream.appendHTML

class HTMLBuilder {
    private var head: Head? = null
    private var scripts: Scripts = Scripts()

    fun head(block: HeadBuilder.() -> Unit) {
        head = HeadBuilder().apply(block).build()
    }

    fun scripts(block: Scripts.() -> Unit) {
        scripts = scripts.apply(block)
    }

    fun build(): HTMLDocument = HTMLDocument(scripts = scripts.scripts, head = head)
}

class HTMLDocument(
    private var head: Head?,
    private var scripts: MutableSet<String>,
) {
    fun buildView(view: BODY.() -> Unit) = StringBuilder().appendHTML().html {
        head {
            head?.meta?.let {
                meta {
                    it.charset
                    name = it.name
                    content = it.content
                }
            }

            link(rel = "icon", href = "data:,") // Disables favicon requests

            head?.stylesheets?.forEach { href ->
                link(href = href, rel = "stylesheet")
            }
        }

        body {
            view()
            scripts.forEach { script ->
                script(type = ScriptType.textJavaScript, src = script) {}
            }
        }
    }
}

class Scripts {
    val scripts = mutableSetOf<String>()

    fun add(script: String) {
        scripts.add(script)
    }

    fun add(scripts: MutableSet<String>) = scripts.addAll(scripts)
}

class Stylesheets {
    val stylesheets = mutableSetOf<String>()

    fun add(stylesheet: String) {
        stylesheets.add(stylesheet)
    }

    fun add(scripts: MutableSet<String>) {
        stylesheets.addAll(scripts)
    }
}

class MetaBuilder {
    var charset: String = "UTF-8"
    var name: String = "viewport"
    var content: String = "width=device-width, initial-scale=1, shrink-to-fit=no"

    fun build(): Meta = Meta(charset = charset, name = name, content = content)
}

class HeadBuilder {
    private var meta: Meta? = Meta()
    private var stylesheets: Stylesheets = Stylesheets()
    var title: String = ""

    fun meta(block: MetaBuilder.() -> Unit) {
        meta = MetaBuilder().apply(block).build()
    }

    fun stylesheets(block: Stylesheets.() -> Unit) = stylesheets.apply(block)
    fun build(): Head = Head(meta = meta, stylesheets = stylesheets.stylesheets, title = title)
}

data class Head(
    val meta: Meta? = Meta(),
    val title: String,
    val stylesheets: MutableSet<String>,
)

class Meta(
    val charset: String = "UTF-8",
    val name: String = "viewport",
    val content: String = "width=device-width, initial-scale=1, shrink-to-fit=no",
)
