package com.healthmetrix.dynamicconsent.commons.pdf

import com.healthmetrix.dynamicconsent.commons.logger
import org.apache.pdfbox.cos.COSBase
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner
import org.apache.pdfbox.pdmodel.interactive.form.PDField
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField
import org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Calendar

fun InputStream.asPDDocument(): PDDocument = PDDocument.load(this)

@Throws(IndexOutOfBoundsException::class, IllegalStateException::class, IOException::class)
fun PDDocument.edit(pageIndex: Int, block: PDPageContentStream.() -> Unit): PDDocument {
    PDPageContentStream(
        this,
        getPage(pageIndex),
        PDPageContentStream.AppendMode.APPEND,
        true,
        true,
    ).use {
        block(it)
    }

    return this
}

fun PDDocument.save(): ByteArray = ByteArrayOutputStream().also {
    save(it)
}.toByteArray()

fun PDDocument.sign(
    signingMaterial: SigningMaterial,
    visibleSignatureOptions: VisibleSignatureOptions? = null,
): ByteArray {
    removeField<PDSignatureField>()

    val signerName = "Healthmetrix GmbH"

    val sig = PDSignature().apply {
        setFilter(PDSignature.FILTER_ADOBE_PPKLITE)
        setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED)
        name = signerName
        signDate = Calendar.getInstance()
    }

    val sigOptions = visibleSignatureOptions?.let { options ->
        val design = PDVisibleSignDesigner(this, options.signature, options.pageNum).apply {
            xAxis(options.x)
            yAxis(getPage(options.pageNum).mediaBox.height - options.y - options.h)
            width(options.w)
            height(options.h)
        }

        val props = PDVisibleSigProperties().apply {
            pdVisibleSignature = design
            page(options.pageNum)
            signerName(signerName)
            visualSignEnabled(true)
        }.also {
            it.buildSignature()
        }

        SignatureOptions().apply {
            setVisualSignature(props.visibleSignature)
            page = options.pageNum
        }
    } ?: SignatureOptions()

    if (visibleSignatureOptions == null) {
        updatePages()
    }

    addSignature(sig, sigOptions)

    val outputStream = ByteArrayOutputStream()

    val signingSupport = saveIncrementalForExternalSigning(outputStream)

    val generator = CMSSignedDataGenerator().apply {
        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider("BC")
            .build(signingMaterial.privateKey)

        val cert = JcaX509v3CertificateBuilder(signingMaterial.publicCert)
            .build(signer)

        addCertificate(cert)
        addSignerInfoGenerator(
            JcaSignerInfoGeneratorBuilder(
                JcaDigestCalculatorProviderBuilder().setProvider("BC").build(),
            ).build(signer, cert),
        )
    }

    val signedData = generator.generate(CMSProcessableInputStream(signingSupport.content))

    signingSupport.setSignature(signedData.getEncoded("DER"))

    close()

    return outputStream.toByteArray()
}

/**
 * non visible signatures do not cause pages to be updated, unlike visible signatures
 * https://issues.apache.org/jira/browse/PDFBOX-45?focusedCommentId=15890614&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-15890614
 *
 * so iterate over pages, set them to be updated, and also update their parents
 */
fun PDDocument.updatePages() {
    documentCatalog.pages.cosObject.isNeedToBeUpdated = true

    pages.forEach { page ->
        var dict = page.cosObject
        while (dict.containsKey(COSName.PARENT)) {
            val parent: COSBase = dict.getDictionaryObject(COSName.PARENT)
            if (parent is COSDictionary) {
                dict.isNeedToBeUpdated = true
                dict = parent
            }
        }
    }
}

// taken from https://github.com/mkl-public/testarea-pdfbox2/blob/master/src/test/java/mkl/testarea/pdfbox2/form/RemoveField.java...
// seems to work, but for some reason adding the second signature
// revives the old signature, which is silly.  In any case, acrobat
// doesn't complain about the broken old signature, it can recognize
// it's an older version, whereas, without removing the field, it
// will throw an error
// Mostly the same, with some Kotlin improvements

@Throws(IOException::class)
inline fun <reified T : PDField> PDDocument.removeField(): PDField? {
    val documentCatalog = documentCatalog
    val acroForm = documentCatalog.acroForm
    if (acroForm == null) {
        logger.warn("No form defined.")
        return null
    }

    var targetField: PDField? = null
    for (field in acroForm.fieldTree) {
        if (field is T) {
            targetField = field
            break
        }
    }
    if (targetField == null) {
        logger.warn("Form does not contain field with given name.")
        return null
    }
    val parentField = targetField.parent
    if (parentField != null) {
        val childFields = parentField.children
        var removed = false
        for (field in childFields) {
            if (field.cosObject == targetField.cosObject) {
                removed = childFields.remove(field)
                parentField.children = childFields
                break
            }
        }
        if (!removed) logger.warn("Inconsistent form definition: Parent field does not reference the target field.")
    } else {
        val rootFields = acroForm.fields
        var removed = false
        for (field in rootFields) {
            if (field.cosObject == targetField.cosObject) {
                removed = rootFields.remove(field)
                break
            }
        }
        if (!removed) logger.warn("Inconsistent form definition: Root fields do not include the target field.")
    }
    targetField.removeWidgets()
    documentCatalog.cosObject.isNeedToBeUpdated = true
    return targetField
}

@Throws(IOException::class)
@PublishedApi
internal fun PDField.removeWidgets() {
    if (this is PDTerminalField) {
        val widgets = widgets
        for (widget in widgets) {
            val page = widget.page
            if (page != null) {
                val annotations = page.annotations
                var removed = false
                for (annotation in annotations) {
                    if (annotation.cosObject == widget.cosObject) {
                        removed = annotations.remove(annotation)
                        break
                    }
                }
                if (!removed) logger.warn("Inconsistent annotation definition: Page annotations do not include the target widget.")
            } else {
                logger.warn("Widget annotation does not have an associated page; cannot remove widget.")
            }
        }
    } else if (this is PDNonTerminalField) {
        val childFields = children
        for (field in childFields) field.removeWidgets()
    } else {
        logger.warn("Target field is neither terminal nor non-terminal; cannot remove widgets.")
    }
}
