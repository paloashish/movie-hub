// movie-service: movies, posters, credits, and the single GraphQL API for the frontend (ADR 0002).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)   // opens @Component/@Transactional classes for Spring proxies
    alias(libs.plugins.kotlin.jpa)      // no-arg constructors for @Entity classes
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":common"))
    implementation(project(":contracts"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-grpc-client")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(libs.springdoc.ui)                     // /swagger-ui.html for the poster endpoint (foundation checklist B)
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation(libs.mockk)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
