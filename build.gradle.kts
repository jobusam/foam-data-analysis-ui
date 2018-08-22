import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.2.61"
    application
}

group = "foam-data"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))

    compile ("no.tornado","tornadofx","1.7.17")

    //For serialization to JSON
    compile ("com.fasterxml.jackson.module","jackson-module-kotlin","2.9.+")

    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "de.foam.data.analysis.FileHistogramAppKt"
}
