import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(tms.plugins.kotlin)
    alias(tms.plugins.kotlin.serialization)
    `java-library`
    `maven-publish`
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
    implementation(libs.kotlixJson)
    testImplementation(tms.junit.launcher)
    testImplementation(tms.junit.api)
    testImplementation(tms.junit.engine)
    testImplementation(tms.kotest.runner)
    testImplementation(tms.kotest.assertion)
    testImplementation(libs.sulkId)
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

val libraryVersion: String = properties["lib_version"]?.toString() ?: "latest-local"

publishing {
    repositories{
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/navikt/tms-utkast")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("gpr") {
            groupId = "no.nav.tms.utkast"
            artifactId = "builder"
            version = libraryVersion
            from(components["java"])

            val sourcesJar by tasks.registering(Jar::class) {
                archiveClassifier.set("sources")
                from(sourceSets.main.get().allSource)
            }
            artifact(sourcesJar)
        }
    }
}
