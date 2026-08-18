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

    /*
     * Paper development bundle for Minecraft/Paper 1.21.11.
     */
    paperweight.paperDevBundle(
        "1.21.11-R0.1-SNAPSHOT"
    )

    /*
     * Kotlin runtime.
     */
    implementation(
        kotlin("stdlib")
    )

    /*
     * Kotlin coroutines.
     *
     * Used for asynchronous application-level work.
     * Bukkit/Paper API access remains on the correct server thread.
     */
    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"
    )

    /*
     * Kotlin serialization.
     */
    implementation(
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0"
    )

    /*
     * SLF4J API supplied by the server environment.
     */
    compileOnly(
        "org.slf4j:slf4j-api:2.0.17"
    )

    /*
     * ============================================================
     * TESTING
     * ============================================================
     *
     * Keep the testing dependencies explicit.
     *
     * Previously we relied on kotlin("test") plus the aggregate
     * JUnit dependency. The GitHub build was not exposing the
     * required test API to compileTestKotlin correctly.
     *
     * Explicit JUnit API + engine removes that ambiguity.
     */

    testImplementation(
        "org.junit.jupiter:junit-jupiter-api:6.0.0"
    )

    testRuntimeOnly(
        "org.junit.jupiter:junit-jupiter-engine:6.0.0"
    )
}

kotlin {

    /*
     * Minecraft/Paper 1.21.11 runs on Java 21.
     */
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(
            JvmTarget.JVM_21
        )

        freeCompilerArgs.addAll(
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

    /*
     * Kotlin compilation.
     */
    compileKotlin {
        compilerOptions {
            jvmTarget.set(
                JvmTarget.JVM_21
            )
        }
    }

    /*
     * Java compilation.
     */
    compileJava {
        options.release.set(21)
    }

    /*
     * JUnit 5/6 platform.
     *
     * JUnit Jupiter tests run through the JUnit Platform.
     */
    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    /*
     * The normal JAR is not the deployment artifact.
     */
    jar {
        enabled = false
    }

    /*
     * Shadow JAR.
     */
    named<ShadowJar>("shadowJar") {

        archiveBaseName.set(
            "MythicInvasion"
        )

        archiveClassifier.set(
            "dev-all"
        )

        duplicatesStrategy =
            DuplicatesStrategy.EXCLUDE

        /*
         * Relocate coroutine packages to prevent
         * dependency collisions with other plugins.
         */
        relocate(
            "kotlinx.coroutines",
            "io.github.mindzard.mythicinvasion.libs.coroutines"
        )

        /*
         * Relocate Kotlin serialization packages.
         */
        relocate(
            "kotlinx.serialization",
            "io.github.mindzard.mythicinvasion.libs.serialization"
        )
    }

    /*
     * Make assemble produce the shaded artifact.
     */
    assemble {
        dependsOn(
            named<ShadowJar>("shadowJar")
        )
    }
}

/*
 * Paperweight reobfuscation.
 *
 * Shadow JAR must exist before the reobfuscation lifecycle
 * completes.
 */
tasks.named("reobfJar") {
    dependsOn(
        tasks.named<ShadowJar>("shadowJar")
    )
}
