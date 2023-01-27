package com.healthmetrix.dynamicconsent.signing.verifyjwt

import com.auth0.jwt.interfaces.DecodedJWT
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.runCatching
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import com.healthmetrix.dynamicconsent.commons.SecretKey
import com.healthmetrix.dynamicconsent.commons.Secrets
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Configuration
@Profile("firebase-token-verifier")
class FirebaseConfig {

    @Bean
    fun provideFirebaseApp(
        secrets: Secrets,
    ): FirebaseApp = FirebaseOptions.builder().apply {
        secrets.get(SecretKey.FIREBASE_SDK_CREDENTIALS)
            .byteInputStream()
            .let(GoogleCredentials::fromStream)
            .let(this::setCredentials)
    }.build().let(FirebaseApp::initializeApp)
}

@Component
@Profile("firebase-token-verifier")
class FirebaseTokenVerifier(
    private val firebaseApp: FirebaseApp,
) : TokenVerifier {
    override val issuers = listOf("https://securetoken.google.com/research-app-c0599")

    override fun verify(encoded: String, decoded: DecodedJWT): Result<JwtInfo, Throwable> = FirebaseAuth
        .getInstance(firebaseApp)
        .runCatching { verifyIdToken(encoded) }
        .map(FirebaseToken::getUid)
        .map(::JwtInfo)
}
