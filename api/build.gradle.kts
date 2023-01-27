@file:Suppress("UnstableApiUsage")

import com.healthmetrix.dynamicconsent.buildlogic.conventions.excludeReflect
import com.healthmetrix.dynamicconsent.buildlogic.conventions.exclusionsSpringTestImplementation
import com.healthmetrix.dynamicconsent.buildlogic.conventions.exclusionsSpringTestRuntime
import com.healthmetrix.dynamicconsent.buildlogic.conventions.registeringExtended
import org.springframework.boot.gradle.tasks.bundling.BootJar

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage> {
    imageName = "healthmetrixgmbh/dynamic-consent"
}

dependencies {
    implementation(projects.commons)
    implementation(projects.consent)
    implementation(projects.signing)
    implementation(projects.persistence)

    implementation(libs.kotlin.reflect)

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.webflux)

    // documentation
    implementation(libs.bundles.springdoc)

    // metrics
    implementation(libs.micrometer.cloudwatch2)

    // jackson
    implementation(libs.jackson.kotlin) { excludeReflect() }
    implementation(libs.jackson.xml) // necessary for logback config
    implementation(libs.jackson.yaml)

    // testing
    testImplementation(libs.bundles.test.spring.implementation) { exclusionsSpringTestImplementation() }
    testRuntimeOnly(libs.bundles.test.spring.runtime) { exclusionsSpringTestRuntime() }
    testImplementation(libs.jwt)

    // generate schema for s4h-consents
    testImplementation(libs.schema.generate)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)
        val acceptance by registeringExtended(test, libs.versions.junit.get()) {}
    }
}

tasks {
    withType<BootJar> {
        setProperty("archiveFileName", "dynamic-consent.jar")
    }
    named("check") {
        dependsOn(testing.suites.named("acceptance"))
    }
}
