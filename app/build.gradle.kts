import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.kotlin)

    application
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    mavenLocal()
}


dependencies {
    implementation(libs.bundles.flyway)
    implementation(libs.hikari.cp)
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.bundles.ktor.client)
    implementation(libs.logback.encoder)
    implementation(libs.tms.ktor.support.entraId)
    implementation(libs.tms.ktor.support.exchange)
    implementation(libs.postgresql.default)
    implementation(libs.kotli.query)
    implementation(libs.bundles.tms.common)
    implementation(libs.prometheus.metric.core)
    implementation(libs.bundles.jackson)
    implementation(libs.tms.kafka.tools.application)
    implementation(project(":lib"))

    testImplementation(libs.junit.launcher)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.engine)
    testImplementation(libs.mockk.default)
    testImplementation(libs.test.container.postgresql)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertion)
    testImplementation(libs.tms.ktor.support.entraId.mock)
    testImplementation(libs.ktor.serialization.kotlinx)
    testImplementation(libs.ktor.server.header)
    testImplementation(libs.ktor.test.client)
    testImplementation(libs.ktor.test.server)
}

application {
    mainClass.set("no.nav.tms.utkast.ApplicationKt")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }
}
