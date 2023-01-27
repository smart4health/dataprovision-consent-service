package com.healthmetrix.dynamicconsent.signing

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesKey(
    private val secretKeySpec: SecretKeySpec,
) {
    fun encrypt(plaintext: ByteArray): ByteArray {
        val iv = with(SecureRandom()) {
            val buf = ByteArray(12)
            nextBytes(buf)
            buf
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, secretKeySpec, GCMParameterSpec(128, iv))
        }

        val cipherText = cipher.doFinal(plaintext)

        val msg = with(ByteBuffer.allocate(4 + iv.size + cipherText.size)) {
            putInt(iv.size)
            put(iv)
            put(cipherText)
        }.array()

        Arrays.fill(iv, 0)

        return msg
    }

    fun decrypt(input: ByteArray): Result<ByteArray, Throwable> = runCatching {
        val (iv, cipherText) = with(ByteBuffer.wrap(input)) {
            val ivLength = int
            if (ivLength !in (12..16)) {
                throw IllegalArgumentException("invalid iv length")
            }

            val iv = ByteArray(ivLength).also { get(it) }
            val cipherText = ByteArray(remaining()).also { get(it) }

            iv to cipherText
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, secretKeySpec, GCMParameterSpec(128, iv))
        }

        cipher.doFinal(cipherText)
    }
}
