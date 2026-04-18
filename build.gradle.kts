plugins {
    java
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))

    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-logging-json")
    implementation("io.quarkus:quarkus-amazon-lambda-http")

    implementation("io.quarkiverse.openapi.generator:quarkus-openapi-generator:2.14.0-lts")

    implementation("io.quarkus:quarkus-rest-client")
    implementation("io.quarkus:quarkus-rest-client-jackson")


    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

group = "com.lan.app"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

sourceSets {
    main {
        java {
            srcDirs("build/classes/java/quarkus-generated-sources/open-api")
        }
    }
}