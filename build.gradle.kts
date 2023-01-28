val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.7.22"
    id("io.ktor.plugin") version "2.2.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.22"
    id("org.jetbrains.kotlin.kapt") version "1.7.0"
    id("io.ebean") version "13.6.0"
}

group = "com.example"
version = "0.0.1"
application {
    mainClass.set("com.example.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("com.hedera.hashgraph:sdk:2.19.0")
    implementation("io.grpc:grpc-netty-shaded:1.46.0")
    implementation("io.github.cdimascio:dotenv-java:2.3.2")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("io.ktor:ktor-server-sessions:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("com.sksamuel.hoplite:hoplite-core:2.7.0")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.7.0")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.1")
    implementation("org.slf4j:slf4j-simple:2.0.3")
    implementation("io.ebean:ebean:13.6.0")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.3")
    kapt("io.ebean:kotlin-querybean-generator:13.6.0")
    implementation("io.ebean:ebean-migration:13.6.0")
    implementation("io.ebean:ebean-ddl-generator:13.6.0")
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ebean:ebean-test:13.6.0")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")

}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
}

tasks.withType<JavaCompile> {
    targetCompatibility = JavaVersion.VERSION_17.toString()
    sourceCompatibility = JavaVersion.VERSION_17.toString()
}

tasks.test {
    useJUnitPlatform()
}

ebean {
    debugLevel = 1
    kotlin = true
}