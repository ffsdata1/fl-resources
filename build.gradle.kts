plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
}

application {
    mainClass.set("MainKt")  // <-- Important!

    applicationDefaultJvmArgs = listOf(
        "--add-opens", "java.base/sun.security.ssl=ALL-UNNAMED"
    )
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(18)
}