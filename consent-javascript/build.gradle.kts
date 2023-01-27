import org.siouan.frontendgradleplugin.FrontendGradlePlugin
import org.siouan.frontendgradleplugin.infrastructure.gradle.AssembleTask
import org.siouan.frontendgradleplugin.infrastructure.gradle.InstallDependenciesTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("java")
    id("idea")
    alias(libs.plugins.frontend.build)
}

frontend {
    nodeVersion.set("16.18.1")
    nodeInstallDirectory.set(project.buildDir.child("node"))

    yarnEnabled.set(true)
    yarnVersion.set("1.22.19")
    yarnInstallDirectory.set(project.buildDir.child("yarn"))

    assembleScript.set("run assemble")
}

idea.module.excludeDirs = idea.module.excludeDirs + file("node_modules")

tasks.withType<Jar> {
    from(project.buildDir.child("dist"))
    includeEmptyDirs = false
    dependsOn(tasks.getByName<AssembleTask>(FrontendGradlePlugin.ASSEMBLE_TASK_NAME))
}

tasks.getByName<AssembleTask>(FrontendGradlePlugin.ASSEMBLE_TASK_NAME) {
    dependsOn(tasks.getByName<InstallDependenciesTask>(FrontendGradlePlugin.INSTALL_TASK_NAME))
}

tasks.getByName<Delete>("clean") {
    delete(file("node_modules"))
}

fun File.child(name: String): File {
    if (!exists() || isDirectory) {
        return File(this, name)
    }

    throw IllegalStateException("Parent file ${this.name} should be directory")
}
