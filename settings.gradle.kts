dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
            name = "Ktor EAP"
        }
        maven {
            url = uri("https://jitpack.io")
            name = "JitPack"
        }
        google()
    }
}

rootProject.name = "schedule"