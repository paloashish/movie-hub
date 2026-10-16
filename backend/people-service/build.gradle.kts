// people-service: cast and creators, reachable over gRPC only (ADR 0002). HTTP is used for actuator health.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":common"))
    implementation(project(":contracts"))

    implementation("org.springframework.boot:spring-boot-starter-grpc-server")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")      // for actuator over HTTP
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-grpc-server-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation(libs.mockk)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
