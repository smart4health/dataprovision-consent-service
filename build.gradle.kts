import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    idea
    alias(libs.plugins.gradle.versions)

    // https://youtrack.jetbrains.com/issue/KT-30276
    alias(libs.plugins.kotlin.jvm) apply false

    // Run local tasks
    id("com.healthmetrix.kotlin.localdriver")
}

allprojects {
    group = "com.healthmetrix"
    version = "1.0.0"
}

tasks {
    withType<DependencyUpdatesTask> {
        outputFormatter = closureOf<Result> {
            val sb = StringBuilder()
            outdated.dependencies.forEach { dep ->
                sb.append("${dep.group}:${dep.name} ${dep.version} -> ${dep.available.release ?: dep.available.milestone}\n")
            }
            if (sb.isNotBlank()) {
                rootProject.file("build/dependencyUpdates/outdated-dependencies").apply {
                    parentFile.mkdirs()
                    println(sb.toString())
                    writeText(sb.toString())
                }
            } else {
                println("Up to date!")
            }
        }

        // no alphas, betas, milestones, release candidates
        // or whatever the heck jaxb-api is using
        rejectVersionIf {
            candidate.isSlf4jSpringBoot2Incompatible or
                candidate.isBreakingChangeJwt or
                candidate.isBreakingChangeFrontendJdk or
                candidate.isWaitingForSpringSixHibernateValidator or
                candidate.isSpringMajorUpdate or
                candidate.version.contains("alpha", ignoreCase = true) or
                candidate.version.contains("beta", ignoreCase = true) or
                candidate.version.contains(Regex("M[0-9]*$")) or
                candidate.version.contains("RC", ignoreCase = true) or
                candidate.version.contains(Regex("b[0-9]+\\.[0-9]+$")) or
                candidate.version.contains("eap", ignoreCase = true)
        }
    }
}

// Spring Boot does not yet support SLF4J 2.0.0 as spring-boot-starter-logging requires StaticLoggerBinder: https://github.com/spring-projects/spring-boot/issues/12649
val ModuleComponentIdentifier.isSlf4jSpringBoot2Incompatible: Boolean
    get() = (group == "org.slf4j") and (version.replace(".", "").toInt() >= 200)

// requires migration and possibly breaks stuff: https://github.com/auth0/java-jwt/blob/master/MIGRATION_GUIDE.md#upgrading-from-v3x---v40
val ModuleComponentIdentifier.isBreakingChangeJwt: Boolean
    get() = (moduleIdentifier.toString() == "com.auth0:java-jwt") and (version.replace(".", "").toInt() >= 400)

// requires migration https://github.com/siouan/frontend-gradle-plugin/releases/tag/v6.0.0
val ModuleComponentIdentifier.isBreakingChangeFrontendJdk: Boolean
    get() = (group == "org.siouan.frontend-jdk11") and (version.replace(".", "").toInt() >= 600)

// has to stay on 6.x.x until spring 6 is Jakarta EE 9 compatible
val ModuleComponentIdentifier.isWaitingForSpringSixHibernateValidator: Boolean
    get() = (group == "org.hibernate.validator") and (version.replace(".", "").replace("Final", "").toInt() >= 700)

val ModuleComponentIdentifier.isSpringMajorUpdate: Boolean
    get() = (
        ((group == "org.springdoc") and (version.replace(".", "").toInt() >= 200))
            or ((group == "org.springframework.boot") and (version.replace(".", "").toInt() >= 300))
            or ((group == "org.springframework") and (version.replace(".", "").toInt() >= 600))
            or ((group == "org.springframework.security") and (version.replace(".", "").toInt() >= 600))
            or ((group == "org.springframework.cloud") and (version.replace(".", "").toInt() >= 400))
            or ((group == "com.ninja-squad") and (version.replace(".", "").toInt() >= 400))
        )

tasks.register<com.healthmetrix.dynamicconsent.buildlogic.localdriver.SignPdfTask>("signPdf")
