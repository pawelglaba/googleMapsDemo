// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false
    //id("com.android.tools") version "8.0.0" apply false // wersja Gradle
    //id("com.google.gms:google-services") version "4.3.15" apply false  // plugin Google Services (do obsługi API)

}
//buildscript {
//    repositories {
//        google()
//        mavenCentral()
//    }
//    dependencies {
//        classpath("com.android.tools.build:gradle:8.0.0") // wersja Gradle
//        classpath("com.google.gms:google-services:4.3.15")  // plugin Google Services (do obsługi API)
//    }
//}

