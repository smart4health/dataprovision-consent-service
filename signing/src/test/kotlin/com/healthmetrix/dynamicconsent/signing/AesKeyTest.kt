package com.healthmetrix.dynamicconsent.signing

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.crypto.spec.SecretKeySpec

class AesKeyTest {

    private val underTest =
        AesKey(SecretKeySpec(ByteArray(16), "AES"))

    @Test
    fun `encrypting some text returns a non empty byte array`() {
        val encrypted = underTest.encrypt("hello world".toByteArray())

        assertThat(encrypted.size).isGreaterThan(0)
    }

    @Test
    fun `decrypting invalid bytes results in Err`() {
        val decrypted = underTest.decrypt(ByteArray(8))

        assertThat(decrypted).isInstanceOf(Err::class.java)
    }

    @Test
    fun `round trip results in original plaintext`() {
        val plaintext = "alice is talking to bob again"
        val res = underTest.encrypt(plaintext.toByteArray(Charsets.UTF_8))
            .let(underTest::decrypt)
            .unwrap()
            .toString(Charsets.UTF_8)

        assertThat(res).isEqualTo(plaintext)
    }
}
