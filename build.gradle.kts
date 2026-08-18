import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.4.10"

    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"

    id("com.gradleup.shadow") version "9.6.1"
}

group = "io.github.mindzard"
version = "0.1.0-SNAPSHOT"

description = "AI-Powered Dynamic Mythic Invasion & Ecosystem Engine"

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {

    paperweight.paperDevBundle(
        "1.21.11-R0.1-SNAPSHOT"
    )

    implementation(
        kotlin("stdlib")
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0"
    )

    /*
     * Official Google Gen AI Java SDK.
     *
     * The AI layer remains disabled by configuration until we
     * explicitly enable it and provide GOOGLE_API_KEY.
     */
    implementation(
        "com.google.genai:google-genai:1.63.0"
    )

    compileOnly(
        "org.slf4j:slf4j-api:2.0.17"
    )

    testImplementation(
        "org.junit.jupiter:junit-jupiter-api:6.0.3"
    )

    testRuntimeOnly(
        "org.junit.jupiter:junit-jupiter-engine:6.0.3"
    )

    testRuntimeOnly(
        "org.junit.platform:junit-platform-launcher"
    )
}

kotlin {

    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(
            JvmTarget.JVM_21
        )

        freeCompilerArgs.add(
            "-Xjsr305=strict"
        )
    }
}

java {

    toolchain {
        languageVersion.set(
            JavaLanguageVersion.of(21)
        )
    }

    withSourcesJar()
}

tasks {

    compileKotlin {
        compilerOptions {
            jvmTarget.set(
                JvmTarget.JVM_21
            )
        }
    }

    compileJava {
        options.release.set(21)
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    jar {
        enabled = false
    }

    named<ShadowJar>("shadowJar") {

        archiveBaseName.set(
            "MythicInvasion"
        )

        archiveClassifier.set(
            "dev-all"
        )

        duplicatesStrategy =
            DuplicatesStrategy.INCLUDE

        relocate(
            "kotlinx.coroutines",
            "io.github.mindzard.mythicinvasion.libs.coroutines"
        )

        relocate(
            "kotlinx.serialization",
            "io.github.mindzard.mythicinvasion.libs.serialization"
        )
    }

    assemble {
        dependsOn(
            named<ShadowJar>("shadowJar")
        )
    }
}

tasks.named("reobfJar") {
    dependsOn(
        tasks.named<ShadowJar>("shadowJar")
    )
}
