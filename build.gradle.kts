buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:1.9.10-1.0.13")
    }
}

plugins {
    id("com.android.application") version "8.7.2" apply false
    id("com.android.library") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
    id("com.osacky.doctor") version "0.8.0" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

apply(plugin = "com.osacky.doctor")
