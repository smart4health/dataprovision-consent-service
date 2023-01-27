package com.healthmetrix.dynamicconsent.commons.pdf

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers
import org.bouncycastle.cms.CMSTypedData
import java.io.InputStream
import java.io.OutputStream

// CMSTypedStream does not implement CMSTypedData...
class CMSProcessableInputStream(
    private val inputStream: InputStream,
) : CMSTypedData {
    override fun getContent(): Any = inputStream

    override fun write(out: OutputStream) {
        inputStream.copyTo(out)
    }

    override fun getContentType(): ASN1ObjectIdentifier = ASN1ObjectIdentifier(CMSObjectIdentifiers.data.id)
}
