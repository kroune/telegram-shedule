@file:Suppress("SpellCheckingInspection")

plugins {
    kotlin("jvm") version "1.8.20"
    id("io.gitlab.arturbosch.detekt") version "1.23.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
    id("org.jetbrains.kotlinx.dataframe") version "0.11.1"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        setUrl("https://jitpack.io")
    }
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.github.svetlichnyiMaxim:kt-telegram-bot:release")
    implementation("org.jetbrains.kotlinx:dataframe:0.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}