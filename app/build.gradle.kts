import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm").version(Kotlin.version)
    kotlin("plugin.allopen").version(Kotlin.version)

    id(Flyway.pluginId) version (Flyway.version)
    id(Shadow.pluginId) version (Shadow.version)

    // Apply the application plugin to add support for building a CLI application.
    application
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    mavenLocal()
}


dependencies {
    implementation(Flyway.core)
    implementation(Hikari.cp)
    implementation(KotlinLogging.logging)
    implementation(Ktor.Server.core)
    implementation(Ktor.Server.netty)
    implementation(Ktor.Server.auth)
    implementation(Ktor.Server.authJwt)
    implementation(Ktor.Server.statusPages)
    implementation(Ktor.Server.contentNegotiation)
    implementation(Ktor.Serialization.jackson)
    implementation(Ktor.Client.core)
    implementation(Ktor.Client.contentNegotiation)
    implementation(Ktor.Client.apache)
    implementation(TmsKtorTokenSupport.tokenXValidation)
    implementation(TmsKtorTokenSupport.tokendingsExchange)
    implementation(Postgresql.postgresql)
    implementation(RapidsAndRivers.rapidsAndRivers)
    implementation(KotliQuery.kotliquery)
    implementation(TmsCommonLib.kubernetes)
    implementation(TmsCommonLib.metrics)
    implementation(TmsCommonLib.observability)
    implementation(TmsCommonLib.utils)
    implementation(project(":lib"))

    testImplementation(JacksonDatatype.moduleKotlin)
    testImplementation(JacksonDatatype.datatypeJsr310)
    testImplementation(Junit.api)
    testImplementation(Junit.engine)
    testImplementation(Mockk.mockk)
    testImplementation(TestContainers.postgresql)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
    testImplementation(Ktor.Test.serverTestHost)
    testImplementation(TmsKtorTokenSupport.tokenXValidationMock)
    testImplementation(KotlinxSerialization.json)
    testImplementation(Ktor.Server.defaultHeaders)

    testImplementation(project(":lib"))
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

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// TODO: Fjern følgende work around i ny versjon av Shadow-pluginet:
// Skal være løst i denne: https://github.com/johnrengelman/shadow/pull/612
project.setProperty("mainClassName", application.mainClass.get())
apply(plugin = Shadow.pluginId)
