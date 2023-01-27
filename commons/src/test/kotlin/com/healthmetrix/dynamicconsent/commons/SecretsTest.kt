package com.healthmetrix.dynamicconsent.commons

import com.amazonaws.secretsmanager.caching.SecretCache
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SecretsTest {
    private val secretCache: SecretCache = mockk()
    private val underTest = AwsSecrets(secretCache, "test/namespace")

    @Test
    fun `it gets expected value`() {
        every { secretCache.getSecretString("test/namespace/signing/database-credentials") } returns "value"
        val result = underTest.get(SecretKey.DB_CREDENTIALS)
        assertThat(result).isEqualTo("value")
    }

    @Test
    fun `throws InternalError exception when secretCache returns null`() {
        val secret = "secret"
        every { secretCache.getSecretString(any()) } returns null
        assertThrows<ResourceNotFoundException>("Failed to retrieve secret $secret") { underTest.get(SecretKey.DB_CREDENTIALS) }
    }
}
