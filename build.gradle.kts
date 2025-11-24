import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.2.20"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "br.com.colman"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    val ktorVersion = "3.0.0"
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")

    testImplementation(kotlin("test"))
    // Kotest for testing
    val kotestVersion = "5.9.1"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    // MockK (not strictly required, but available if needed)
    testImplementation("io.mockk:mockk:1.13.13")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application {
    // The main function is in src/main/kotlin/Main.kt
    mainClass.set("MainKt")
}

// Configure the Shadow JAR (fat JAR) so we can run the app with `java -jar`
tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("all")
    // Ensure the manifest points to the application's main class
    manifest {
        attributes(mapOf("Main-Class" to application.mainClass.get()))
    }
}

// Make `./gradlew build` also produce the shadow jar when needed
tasks.named("build") {
    dependsOn("shadowJar")
}