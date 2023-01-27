package com.healthmetrix.dynamicconsent.commons

import com.amazonaws.secretsmanager.caching.SecretCache
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.Base64

enum class SecretKey(
    val location: String,
) {
    DB_CREDENTIALS("signing/database-credentials"),
    CONSENT_SIGNING_MATERIAL("consent/signing-material"),
    SIGNING_SIGNING_MATERIAL("signing/signing-material"),
    SIGNING_ENCRYPTION_KEY("signature-token-encryption-key"),
    FIREBASE_SDK_CREDENTIALS("service-account-key"),
}

interface Secrets {
    fun get(key: SecretKey): String
}

@Service
@Profile("secrets-aws")
class AwsSecrets(
    private val secretCache: SecretCache,
    @Value("\${secrets.namespace}")
    namespace: String,
) : Secrets {

    private val namespace: String

    init {
        if (namespace.endsWith("/")) {
            logger.warn("secrets.namespace should not end with /, trimming")
        }

        this.namespace = namespace.trimEnd('/')
    }

    override fun get(key: SecretKey): String {
        val id = key.location
        if (id.startsWith("/")) {
            logger.warn("secret id $id starts with /, trimming")
        }

        val fullId = "$namespace/${id.trimStart('/')}"

        return try {
            secretCache.getSecretString(fullId)
                ?: throw ResourceNotFoundException("Failed to retrieve secret $fullId")
        } catch (ex: ResourceNotFoundException) {
            logger.warn("Failed to retrieve secret $fullId")
            throw ex
        }
    }
}

@Service
@Profile("secrets-vault")
class VaultSecrets(
    private val rdsCredentials: SecretsConfig.RdsCredentials,
    private val consentSigningMaterial: SecretsConfig.ConsentSigningMaterial,
    private val signingSigningMaterial: SecretsConfig.SigningSigningMaterial,
    private val signingEncryption: SecretsConfig.SigningEncryption,
    private val firebase: SecretsConfig.Firebase,
) : Secrets {
    override fun get(key: SecretKey): String = when (key) {
        SecretKey.DB_CREDENTIALS -> jsonString {
            "username" to rdsCredentials.username
            "password" to rdsCredentials.password
        }
        SecretKey.CONSENT_SIGNING_MATERIAL -> jsonString {
            "private-key" to consentSigningMaterial.privateKey
            "public-cert" to consentSigningMaterial.publicCert
        }
        SecretKey.SIGNING_SIGNING_MATERIAL -> jsonString {
            "private-key" to signingSigningMaterial.privateKey
            "public-cert" to signingSigningMaterial.publicCert
        }
        SecretKey.SIGNING_ENCRYPTION_KEY -> signingEncryption.encryptionKey
        SecretKey.FIREBASE_SDK_CREDENTIALS -> firebase.serviceAccountKey
    }
}

@Service
@Profile("!secrets-aws & !secrets-vault")
class MockSecrets : Secrets {
    override fun get(key: SecretKey): String = when (key) {
        SecretKey.DB_CREDENTIALS -> jsonString {
            "username" to "username"
            "password" to "password"
        }
        SecretKey.SIGNING_ENCRYPTION_KEY -> Base64.getEncoder().encodeToString(ByteArray(16))
        SecretKey.CONSENT_SIGNING_MATERIAL, SecretKey.SIGNING_SIGNING_MATERIAL -> jsonString {
            "private-key" to EXAMPLE_PRIVATE_RSA_KEY.encodeBase64()
            "public-cert" to EXAMPLE_X509_CERTIFICATE.encodeBase64()
        }
        SecretKey.FIREBASE_SDK_CREDENTIALS -> throw Exception("Supply key manually to test this locally")
    }
}

@Configuration
class SecretsConfig {

    @Bean
    @Profile("secrets-aws")
    fun provideAWSSecretCache(): SecretCache = SecretCache()

    @Profile("secrets-vault")
    @ConstructorBinding
    @ConfigurationProperties("rds-credentials")
    data class RdsCredentials(
        val username: String,
        val password: String,
    )

    @Profile("secrets-vault")
    @ConstructorBinding
    @ConfigurationProperties("signing-signing-material")
    data class SigningSigningMaterial(
        val privateKey: String,
        val publicCert: String,
    )

    @Profile("secrets-vault")
    @ConstructorBinding
    @ConfigurationProperties("consent-signing-material")
    data class ConsentSigningMaterial(
        val privateKey: String,
        val publicCert: String,
    )

    @Profile("secrets-vault")
    @ConstructorBinding
    @ConfigurationProperties("signing-encryption")
    data class SigningEncryption(
        val encryptionKey: String,
    )

    @Profile("secrets-vault")
    @ConstructorBinding
    @ConfigurationProperties("firebase")
    data class Firebase(
        val serviceAccountKey: String,
    )
}

// this and the example public cert generated with, and identifying info filled out accordingly
// `openssl req -x509 -nodes -newkey rsa:2048 -keyout private.key -out public.cert -days 365`
val EXAMPLE_PRIVATE_RSA_KEY =
    """
        -----BEGIN PRIVATE KEY-----
        MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDsbK+zIaL1UJ8D
        A6jspzHGFd//PHeEh4GxNXyTt3/lBVDPns4rDSIgBBIhJ1SOUdCE6VJOwypNLivo
        /C2ov2pUAqo5ekQdKfcf4LvlOJG21XJIo0tpTvEYHP4ea6oKVO60Lx6+OxIeaR5l
        ded4M/avMT3HfoJqEV2cUhXEWLr4zudpJ8W9LR2hFbycMDA4LnMhNrQu7bSy8K8W
        Kj8CuNjLl668ePE0k7r+Wi5suVuDI3jgpJhAkFzpvs2nmZgm5GvdRtkjd55rbq7A
        B2Ie0ETP0dnD1lEYRK4AuIqOefXRh4Hou5BPN6QWVLGgxiBTaN/7IzJJcvMx/H/z
        uF4Mn3rlAgMBAAECggEBAILk+bG3Yb1n0WGd2RyMxKXi3o01MQaS717pOfuekJR/
        iOoqBgMX1ljbaB4q5EmXSlcLxqIMs5LfiTgIxk6zC29PumbwJl3vKf7gMacK1sHf
        EtLcdZfsONcc1iSJfy8RLsdbfy3bDoD/ufB6gpiuTFlabdXRWOrYindNiveWGI4/
        DryCbV41aAPuGRLuG5zj/7O64Xx/GgatWWqzfpxwJbOIABjVXx+J8O94XXnvLRyf
        RSC4QIVIuTylEkVR6xmQ7b+D6eaDopb1AvBP4I2o2+U9BmqmeKy8KZaSvXChGUVV
        hmF/cRc4bfjPGHLWQKZcfu4D+/axQImOcOuDtHL8LtECgYEA91KT+bW7njeTnUx5
        ysFtYEHba8RJnqtc6L+yO28rII99nB7svGGosL67BrUsjJhcHsfqX0iAVf0z26KZ
        DfUvp295FXmjdlNlkL8x8XLFUf2om4T7feJZ5Ui6Jh0Wll4u/gTI3VYnNpP1dmdr
        5oTKTsIhtQAB6eNsnXFmLbdCoysCgYEA9Lg5P4lUKNiv8otZ1s9J6sVuRH+FXOkE
        drlEVKE/+1H9biICronCe2E5hZEFzUgbBGpOk2i3HE50xUygshNBLPWYakSO3UJ0
        r3L4mTAN7dbPq2jmJZFD5GKQYjfFAcPmOAl/l6Kqn5DmEPZc8FAteTAuGBfDOUOe
        g7K+yVx/ki8CgYBtDwHndAbGjtVN4KI4ETFM59182PJpwEDY9Wb/pFbNJdkK1ewo
        aZv8TC6ml4+Mc8bzX85us8a7pEqQyFNbf0nDOHmZSakWDVJVEKpSQPzX2dXWtj47
        1Ws0muxS0DLcna11H+D/EPV7sKTl0FcZMGcRcbNxYA5392w2xQF8mToOPwKBgQCT
        CzsC7W6PWJxoXckupMFiex0ltYYZ7L3M5MelHnsGdR6VlYQNAiPE5QeeNSTKU4BA
        Z+ws8OfN72qEvuVz+tPXwv7BI87ALlVUsv2jdld4SPHhqhdYDXBBA+SDz8TlqaNk
        mnh7Ube6R2OmX5I6p8KbJhMcUPLyLqpJshangZf56wKBgH7YQqQdHOhdy5Wl/DPI
        thGg3sLFb4r1nVWcetOWR+IskcdElJOCKV6WsJ9CjEv+bY83+9q9pIKwXrDjytW/
        JeD5mWmrUlo7sdpRAbijUax7+QumC68Vq+Wn2OKVt4+rbVlPPsUicJJQ/WxPtNVm
        5H2JhUbGiNSxUnS5m+ADctdo
        -----END PRIVATE KEY-----
    """.trimIndent()

val EXAMPLE_X509_CERTIFICATE =
    """
        -----BEGIN CERTIFICATE-----
        MIIDozCCAougAwIBAgIUCMjzSa8pnYVAuW7a6QMaIP74JN4wDQYJKoZIhvcNAQEL
        BQAwYTELMAkGA1UEBhMCREUxDzANBgNVBAgMBkJlcmxpbjEPMA0GA1UEBwwGQmVy
        bGluMRUwEwYDVQQKDAxIZWFsdGhtZXRyaXgxGTAXBgNVBAMMEGhlYWx0aG1ldHJp
        eC5jb20wHhcNMjAwNzIzMTMyMjUwWhcNMjEwNzIzMTMyMjUwWjBhMQswCQYDVQQG
        EwJERTEPMA0GA1UECAwGQmVybGluMQ8wDQYDVQQHDAZCZXJsaW4xFTATBgNVBAoM
        DEhlYWx0aG1ldHJpeDEZMBcGA1UEAwwQaGVhbHRobWV0cml4LmNvbTCCASIwDQYJ
        KoZIhvcNAQEBBQADggEPADCCAQoCggEBAOxsr7MhovVQnwMDqOynMcYV3/88d4SH
        gbE1fJO3f+UFUM+ezisNIiAEEiEnVI5R0ITpUk7DKk0uK+j8Lai/alQCqjl6RB0p
        9x/gu+U4kbbVckijS2lO8Rgc/h5rqgpU7rQvHr47Eh5pHmV153gz9q8xPcd+gmoR
        XZxSFcRYuvjO52knxb0tHaEVvJwwMDgucyE2tC7ttLLwrxYqPwK42MuXrrx48TST
        uv5aLmy5W4MjeOCkmECQXOm+zaeZmCbka91G2SN3nmtursAHYh7QRM/R2cPWURhE
        rgC4io559dGHgei7kE83pBZUsaDGIFNo3/sjMkly8zH8f/O4XgyfeuUCAwEAAaNT
        MFEwHQYDVR0OBBYEFN7zppHiPZFOtctW+vZxDa15kBWfMB8GA1UdIwQYMBaAFN7z
        ppHiPZFOtctW+vZxDa15kBWfMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQEL
        BQADggEBAN9VpEOKtZY3Mqb77SmCbliqGGi5PxXm5fipTgLOMC33PLIehEFMuZoZ
        BZxqa2UQUOWpCAXLbzXbsPwPfU9nqjQijqt0ZQHhXLlFHCVN68phtl6wP+1GpAWt
        e4Y0rSPXFaNxBy7V2rkDwxIQ3aTmXhLNyM/VRVSODNeBNVFBNX7yxR+MkTrc9Rar
        Pdo0k5BTx9NYs05Y9NwEsnEYzqygwLt4y6UC562Gt9HP/gvGBM7IabHGJEaM30ig
        d5MXea+GhISnwLOJ3A3CxY40OI/bB4LITDVBsbXxjxxbG8YoB95LDyGmdxrznu8n
        MmiFebD7+1QIYBBXOSu5FQg9GrXb3Bs=
        -----END CERTIFICATE-----
    """.trimIndent()
