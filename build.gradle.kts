import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
}

group = "dev.altonotes.cohttp"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // Coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // Commons logging
    implementation("commons-logging:commons-logging:1.2")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}