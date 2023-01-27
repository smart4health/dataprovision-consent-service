package com.healthmetrix.dynamicconsent.buildlogic.localdriver

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.UUID
import javax.imageio.ImageIO

/**
 * Task to generate a signed pdf from a locally running consent server
 *
 * usage: ./gradlew signPdf --name="First Last" [--dimensions "WxH"]
 *
 * - long name:  ./gradlew signPdf --name="Averyvery Longnametosign" --consentId="smart4health-research-consent-en" --sigLong
 * - short name:  ./gradlew signPdf --name="Short Name" --consentId="smart4health-research-consent-en" --sig
 * - signature box:  ./gradlew signPdf --name="Short Name" --consentId="smart4health-research-consent-en" --dimensions="200x80"
 *
 * Where the name is two words separated by a space and the dimensions are
 * two positive integers separated  by "x"
 *
 * Options can be added like --options="true,false,true,false" with 1 for yes and 0 for no, providing at least as many options as in the used consent.
 */
open class SignPdfTask : DefaultTask() {

    @Input
    @set:Option(option = "name", description = "The first and last name to use, separated by spaces")
    var patientName: String = ""

    @Input
    @set:Option(option = "dimensions", description = "The dimensions of the signature box, WxH, default '200x80'")
    var dimensions: String = "200x80"

    @Input
    @set:Option(option = "sig", description = "Use a signature.png instead of a black signature box to sign the pdf")
    var sigShort: Boolean = false

    @Input
    @set:Option(
        option = "sigLong",
        description = "Use a signature.png instead of a black signature box to sign the pdf",
    )
    var sigLong: Boolean = false

    @Input
    @set:Option(option = "consentId", description = "Which consent id to use, default smart4health-research-consent-en")
    var consentId: String = "smart4health-research-consent-en"

    @Input
    @set:Option(
        option = "options",
        description = "yes or no for options, e.g. 'true,false,true' is three options: #1 Yes, #2 No, #3 Yes",
    )
    var options: String = "false,false,false,false,false"

    @TaskAction
    fun run() {
        val (firstName, lastName) = patientName.split(' ')
        val (width, height) = dimensions.split('x').map(String::toInt)
        val authHeader = "Bearer ${UUID.randomUUID()}"
        println("authHeader: $authHeader")

        val opt = mapOf(
            "options" to options.split(',')
                .mapIndexed { index, consented -> mapOf("optionId" to index, "consented" to consented.toBoolean()) },
        )
        println("using options: $opt")

        val client = OkHttpClient()
        val objectMapper = ObjectMapper()

        val getUnsignedPdfRequest = Request.Builder().apply {
            url("http://localhost:8080/api/v1/consents/$consentId/documents")
            post(objectMapper.writeValueAsString(opt).toRequestBody("application/json".toMediaType()))
        }.build()

        println("Getting unsigned pdf")
        val unsignedPdf = client.newCall(getUnsignedPdfRequest).execute().body?.bytes()!!

        val prepareSigningRequest = Request.Builder().apply {
            url("http://localhost:8080/api/v1/signatures")
            addHeader("Authorization", authHeader)
            addHeader("X-Hmx-Success-Redirect-Url", "http://localhost:8080")
            addHeader("X-Hmx-Consent-Id", consentId)

            post(unsignedPdf.toRequestBody("application/pdf".toMediaType()))
        }.build()

        println("Preparing for signing")
        val (documentId, token) = client.newCall(prepareSigningRequest).execute().body?.string()!!.let {
            val typeRef = object : TypeReference<Map<String, String>>() {}

            val map = objectMapper.readValue(it, typeRef)
            map.getValue("documentId") to map.getValue("token")
        }

        val sig = if (sigShort) {
            File("build-logic/local-driver/src/main/resources/signature.png").readBytes()
        } else if (sigLong) {
            File("build-logic/local-driver/src/main/resources/signature-long.png").readBytes()
        } else {
            generateSignature(width, height)
        }

        val encoded = Base64.getEncoder().encodeToString(sig)

        val signBody = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "token" to token,
            "signature" to "data:image/png;base64,$encoded",
        )
        val signRequest = Request.Builder().apply {
            url("http://localhost:8080/api/v1/signatures/$documentId/sign")
            put(objectMapper.writeValueAsString(signBody).toRequestBody("application/json".toMediaType()))
        }.build()

        println("Signing")
        client.newCall(signRequest).execute().body?.close()

        val getSignedDocumentRequest = Request.Builder().apply {
            url("http://localhost:8080/api/v2/signatures?consentId=$consentId")
            addHeader("Authorization", authHeader)
            addHeader("Content-Type", "application/octet-stream")
        }.build()

        println("Getting signed document")
        val signedPdf = client.newCall(getSignedDocumentRequest).execute().body?.bytes()!!

        val output = File(project.buildDir, "signed_$consentId.pdf")
        output.parentFile.mkdirs()
        output.writeBytes(signedPdf)
        println("Output signed PDF to ${output.path}")
    }

    private fun generateSignature(width: Int, height: Int): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        (0 until width).forEach { x ->
            img.setRGB(x, 0, Color.BLACK.rgb)
            img.setRGB(x, 1, Color.BLACK.rgb)
            img.setRGB(x, height - 2, Color.BLACK.rgb)
            img.setRGB(x, height - 1, Color.BLACK.rgb)
        }

        (0 until height).forEach { y ->
            img.setRGB(0, y, Color.BLACK.rgb)
            img.setRGB(1, y, Color.BLACK.rgb)
            img.setRGB(width - 2, y, Color.BLACK.rgb)
            img.setRGB(width - 1, y, Color.BLACK.rgb)
        }

        val output = ByteArrayOutputStream()
        ImageIO.write(img, "png", output)

        return output.toByteArray()
    }
}
