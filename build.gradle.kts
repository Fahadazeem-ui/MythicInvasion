import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.testing.Test
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
     * Paperweight provides the Paper API plus access to Paper's
     * development mappings/internal server classes.
     *
     * Minecraft/Paper 1.21.11 belongs to the pre-26.1 generation,
     * so the 1.21.11 dev bundle is still used here.
     */
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")

    /*
     * Kotlin standard library.
     *
     * The Kotlin Gradle plugin already supplies the runtime pieces
     * required by Kotlin code during compilation.
     */
    implementation(kotlin("stdlib"))

    /*
     * Kotlin coroutines.
     *
     * These will become the asynchronous execution layer for:
     * - AI requests
     * - database work
     * - CPU-heavy simulations
     * - background analysis
     *
     * They will NOT be used to directly touch unsafe Bukkit API
     * objects from worker threads.
     */
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    /*
     * JSON serialization.
     *
     * Kept in the foundation because virtually every advanced
     * subsystem will eventually need structured serialization.
     */
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    /*
     * SLF4J API.
     *
     * Paper already provides logging infrastructure, so this remains
     * an API dependency rather than a second logging framework.
     */
    compileOnly("org.slf4j:slf4j-api:2.0.17")

    /*
     * JUnit 5 for automated tests.
     */
    testImplementation(kotlin("test"))

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)

        freeCompilerArgs.addAll(
            "-Xjsr305=strict"
        )
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    withSourcesJar()
}

tasks {
    /*
     * Kotlin/JVM compilation.
     */
    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    /*
     * Java compiler configuration.
     */
    compileJava {
        options.release.set(21)
    }

    /*
     * Unit testing.
     */
    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    /*
     * The normal JAR is intentionally not the release artifact.
     * The shaded/reobfuscated JAR produced later is the deployable one.
     */
    jar {
        enabled = false
    }

    /*
     * Shadow JAR.
     *
     * The name is predictable, which makes GitHub Actions and
     * deployment scripts much easier to maintain.
     */
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("MythicInvasion")
        archiveClassifier.set("dev-all")

        /*
         * Prevent duplicate META-INF resources from breaking the
         * final artifact.
         */
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        /*
         * Relocate Kotlin/coroutines packages so our plugin's bundled
         * libraries cannot accidentally collide with another plugin.
         */
        relocate(
            "kotlinx.coroutines",
            "io.github.mindzard.mythicinvasion.libs.coroutines"
        )

        relocate(
            "kotlinx.serialization",
            "io.github.mindzard.mythicinvasion.libs.serialization"
        )
    }

    /*
     * Clean build:
     *
     * build -> shadow -> paperweight reobfuscation
     *
     * The exact Paperweight task wiring is intentionally delegated to
     * Paperweight rather than manually manipulating Minecraft mappings.
     */
    assemble {
        dependsOn(named<ShadowJar>("shadowJar"))
    }
}

/*
 * Paperweight handles the final transformation for the 1.21.11
 * runtime environment.
 *
 * Keeping this configuration minimal is intentional: Paper's build
 * tooling owns the mapping/reobfuscation lifecycle.
 */
tasks.named("reobfJar") {
    dependsOn(tasks.named<ShadowJar>("shadowJar"))
}
