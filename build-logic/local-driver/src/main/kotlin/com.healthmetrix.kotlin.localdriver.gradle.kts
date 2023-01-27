import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // version is determined by the implementation dependency of build-logic
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain {
        this.languageVersion.convention(JavaLanguageVersion.of(17))
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf("-Xjsr305=strict")
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
