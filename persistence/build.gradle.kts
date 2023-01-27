import com.healthmetrix.dynamicconsent.buildlogic.conventions.excludeReflect
import com.healthmetrix.dynamicconsent.buildlogic.conventions.exclusionsSpringTestImplementation
import com.healthmetrix.dynamicconsent.buildlogic.conventions.exclusionsSpringTestRuntime

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
}

dependencies {
    implementation(projects.commons)
    implementation(projects.signing)
    implementation(projects.persistence.signingApi)

    implementation(libs.kotlin.reflect)

    // spring data
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.types.hibernate)
    runtimeOnly(libs.postgres)

    implementation(libs.jackson.kotlin) { excludeReflect() }

    // testing
    testImplementation(libs.bundles.test.spring.implementation) { exclusionsSpringTestImplementation() }
    testRuntimeOnly(libs.bundles.test.spring.runtime) { exclusionsSpringTestRuntime() }

    implementation(libs.jwt)

    // liquibase
    runtimeOnly(libs.bundles.liquibase)
}
