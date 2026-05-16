plugins {
    kotlin("jvm") version "2.0.21"
    application
    kotlin("plugin.serialization") version "2.0.21"
}

group = "com.catylst.cli"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.materialkolor:material-color-utilities:3.0.0-beta01")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.21")
}

application {
    mainClass.set("com.catylst.cli.CatylstCliKt")
}

tasks.test {
    useJUnit()
}

kotlin {
    jvmToolchain(17)
}
