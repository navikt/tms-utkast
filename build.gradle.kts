plugins {
    kotlin("jvm").version(Kotlin.version)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks {
    jar {
        enabled = false
    }
}

val libraryVersion = properties["lib_version"] ?: "latest-local"

subprojects {
    group = "no.nav"
    version = libraryVersion
}
