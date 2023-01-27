import com.healthmetrix.dynamicconsent.buildlogic.conventions.excludeReflect
import com.healthmetrix.dynamicconsent.buildlogic.conventions.exclusionsSpringTestImplementation
import com.healthmetrix.dynamicconsent.buildlogic.conventions.exclusionsSpringTestRuntime

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(projects.commons)
    implementation(projects.consentJavascript)

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.html)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.jackson.kotlin) { excludeReflect() }
    implementation(libs.jackson.yaml)
    implementation(libs.pdfbox)

    runtimeOnly(libs.bundles.hibernate.validator)

    implementation(libs.bundles.springdoc)

    testImplementation(libs.bundles.test.spring.implementation) { exclusionsSpringTestImplementation() }
    testRuntimeOnly(libs.bundles.test.spring.runtime) { exclusionsSpringTestRuntime() }
}
