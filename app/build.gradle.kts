import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(defaults.plugins.kotlin)

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
    implementation(defaults.bundles.database)
    implementation(defaults.hikariCp)
    implementation(defaults.kotlinLogging)
    implementation(defaults.bundles.ktorServer)
    implementation(defaults.ktor.serialization.jackson)
    implementation(defaults.bundles.ktorClient)
    implementation(defaults.logback.encoder)
    implementation(defaults.tms.ktorTokenSupport.entraId.fetcher)
    implementation(defaults.tms.ktorTokenSupport.userToken.exchange)
    implementation(defaults.postgresql)
    implementation(defaults.kotliQuery)
    implementation(defaults.tms.commonLib.kubernetes)
    implementation(defaults.tms.commonLib.observability)
    implementation(defaults.tms.commonLib.postgres)
    implementation(defaults.tms.commonLib.metrics)
    implementation(defaults.tms.commonLib.utils)
    implementation(defaults.tms.ktorTokenSupport.userToken.verification)
    implementation(defaults.tms.commonLib.teamLogger )
    implementation(defaults.micrometer.registry.prometheus)
    implementation(defaults.bundles.jackson)
    implementation(defaults.tms.kafka.application)
    implementation(defaults.tms.ktorTokenSupport.entraId.verification )
    implementation(project(":lib"))

    testImplementation(defaults.junit.launcher)
    testImplementation(defaults.junit.api)
    testImplementation(defaults.junit.engine)
    testImplementation(defaults.mockk)
    testImplementation(defaults.testContainers.postgresql)
    testImplementation(defaults.kotest.runner)
    testImplementation(defaults.kotest.assertion)
    testImplementation(defaults.tms.ktorTokenSupport.userToken.verificationMock)
    testImplementation(defaults.kotlinx.coroutines)
    testImplementation(defaults.ktor.server.defaultHeaders)
    testImplementation(defaults.ktor.test.clientMock)
    testImplementation(defaults.ktor.test.server)
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
