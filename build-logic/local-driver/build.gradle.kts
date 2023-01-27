plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(mainLibs.kotlin.gradle.plugin)

    implementation(mainLibs.okhttp3.okhttp)
    implementation(mainLibs.jackson.databind)
}
