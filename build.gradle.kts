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
    implementation("io.quarkiverse.openapi.generator:quarkus-openapi-generator:2.14.0-lts")

    implementation("io.quarkus:quarkus-rest-client")
    implementation("io.quarkus:quarkus-rest-client-jackson")

implementation("org.slf4j:slf4j-api:1.7.25")


    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.wiremock:wiremock-standalone:3.13.1")
    testImplementation("org.assertj:assertj-core:3.27.3")
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

tasks.withType<Test> {
    // Environment variables win over both the repo's `.env` file (auto-loaded by Quarkus,
    // contains real secrets) and application.yml's `${VAR:default}` placeholders, so this
    // is the only reliable way to force deterministic dummy config values in tests.
    environment(
        mapOf(
            "TG_BOT_TOKEN" to "test-bot-token",
            "TG_ADMIN_CHAT_ID" to "999999",
            "NOTIFY_SECRET" to "test-notify-secret",
            "APP_SITE_URL" to "https://example.test",
            "APP_BACKEND_URL" to "http://localhost:0",
            "API_TOKEN" to "test-api-token",
            "WIFI_GUEST_PASSWORD" to "guest-pass",
            "WIFI_RESIDENT_PASSWORD" to "resident-pass",
            "PAYMENT_CARD_NUMBER" to "0000 0000 0000 0000"
        )
    )
}