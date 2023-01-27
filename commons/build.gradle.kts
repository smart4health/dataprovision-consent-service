import com.healthmetrix.dynamicconsent.buildlogic.conventions.excludeReflect
import com.healthmetrix.dynamicconsent.buildlogic.conventions.exclusionsSpringTestImplementation
import com.healthmetrix.dynamicconsent.buildlogic.conventions.exclusionsSpringTestRuntime

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.jackson.kotlin) { excludeReflect() }
    implementation(libs.jackson.yaml)

    implementation(libs.kotlinx.html)
    implementation(libs.pdfbox)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)

    // secrets-aws
    implementation(libs.aws.secretsmanager)

    // secrets-vault
    implementation(libs.spring.cloud.vault.config)

    runtimeOnly(libs.spring.ext.reactor)

    api(libs.json)
    api(libs.result)
    api(libs.logback.encoder)
    api(libs.slf4j.api) {
        version {
            strictly("[1.7, ${libs.versions.slf4j.max.get()}[")
            prefer(libs.versions.slf4j.prefer.get())
        }
    }
    api(libs.micrometer.prometheus)

    // testing
    testImplementation(libs.bundles.test.spring.implementation) { exclusionsSpringTestImplementation() }
    testRuntimeOnly(libs.bundles.test.spring.runtime) { exclusionsSpringTestRuntime() }
}
