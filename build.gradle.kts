plugins {
    kotlin("jvm") version "1.8.20"
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
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.1.0")
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