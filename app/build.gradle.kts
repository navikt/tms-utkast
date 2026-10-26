import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(tms.plugins.kotlin)

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
    implementation(tms.bundles.flyway)
    implementation(tms.hikari.cp)
    implementation(tms.kotlin.logging)
    implementation(tms.bundles.ktor.server)
    implementation(tms.ktor.serialization.jackson)
    implementation(tms.bundles.ktor.client)
    implementation(tms.logback.encoder)
    implementation(tms.tms.ktor.support.entraId)
    implementation(tms.tms.ktor.support.exchange)
    implementation(tms.postgresql.default)
    implementation(tms.kotli.query)
    implementation(tms.bundles.tms.common)
    implementation(tms.prometheus.metric.core)
    implementation(tms.bundles.jackson)
    implementation(tms.tms.kafka.tools.application)
    implementation(tms.tms.ktor.support.verification)
    implementation(project(":lib"))

    testImplementation(tms.junit.launcher)
    testImplementation(tms.junit.api)
    testImplementation(tms.junit.engine)
    testImplementation(tms.mockk.default)
    testImplementation(tms.test.container.postgresql)
    testImplementation(tms.kotest.runner)
    testImplementation(tms.kotest.assertion)
    testImplementation(tms.tms.ktor.support.entraId.mock)
    testImplementation(tms.ktor.serialization.kotlinx)
    testImplementation(tms.ktor.server.header)
    testImplementation(tms.ktor.test.client)
    testImplementation(tms.ktor.test.server)
    testImplementation(tms.tms.ktor.support.verification.mock)
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
