package com.healthmetrix.dynamicconsent.commons.pdf

import com.github.michaelbull.result.Result
import org.bouncycastle.util.io.pem.PemReader
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import com.github.michaelbull.result.runCatching as catch

data class SigningMaterial(
    val privateKey: PrivateKey,
    val publicCert: X509Certificate,
)

fun String.decodePrivateKey(): Result<PrivateKey, Throwable> = catch {
    with(KeyFactory.getInstance("RSA")) {
        byteInputStream()
            .reader()
            .let(::PemReader)
            .readPemObject()!!
            .content
            .let(::PKCS8EncodedKeySpec)
            .let(this::generatePrivate)
    }
}

fun String.decodeX509Certificate(): Result<X509Certificate, Throwable> = catch {
    with(CertificateFactory.getInstance("X.509")) {
        byteInputStream()
            .let(this::generateCertificate)
    } as X509Certificate
}
