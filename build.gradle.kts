import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
}

group = "org.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.mysql:mysql-connector-j:8.2.0")
    implementation("io.asyncer:r2dbc-mysql:1.1.0")
    // -- testing --
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // For prettier logging.
    afterSuite(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
        // Only execute on the outermost suite.
        if (desc.parent == null) {
            println("Tests: ${result.testCount}")
            println("Passed: ${result.successfulTestCount}")
            println("Failed: ${result.failedTestCount}")
            println("Skipped: ${result.skippedTestCount}")
        }
    }))
}