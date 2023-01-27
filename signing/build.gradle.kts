import com.healthmetrix.dynamicconsent.buildlogic.conventions.excludeReflect
import com.healthmetrix.dynamicconsent.buildlogic.conventions.excludeSlf4j
import com.healthmetrix.dynamicconsent.buildlogic.conventions.exclusionsSpringTestImplementation
import com.healthmetrix.dynamicconsent.buildlogic.conventions.exclusionsSpringTestRuntime

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(projects.commons)
    implementation(projects.signingJavascript)
    implementation(projects.persistence.signingApi)

    implementation(libs.kotlin.reflect)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.kotlin) { excludeReflect() }
    implementation(libs.jackson.yaml)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.kotlinx.html)
    implementation(libs.pdfbox)
    implementation(libs.jwt)
    implementation(libs.firebase.admin) { excludeSlf4j() }

    runtimeOnly(libs.bundles.hibernate.validator)

    // docs
    implementation(libs.bundles.springdoc)

    // testing
    testImplementation(libs.bundles.test.spring.implementation) { exclusionsSpringTestImplementation() }
    testRuntimeOnly(libs.bundles.test.spring.runtime) { exclusionsSpringTestRuntime() }
    testImplementation(libs.bouncycastle.bcprov)
}
