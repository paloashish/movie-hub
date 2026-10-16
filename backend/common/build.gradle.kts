// Code shared by both services: the error hierarchy and the request-ID plumbing (ADR 0007, application-architecture 4.3).
// Deliberately small. It knows about servlet, gRPC and SLF4J APIs but not about Spring, so each service registers the pieces itself.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(platform(libs.spring.boot.bom))      // Boot-managed versions, nothing pinned here
    implementation("org.slf4j:slf4j-api")
    implementation("io.grpc:grpc-api")
    compileOnly("jakarta.servlet:jakarta.servlet-api")  // provided by the service at runtime

    testImplementation(project(":contracts"))           // a real MethodDescriptor for the gRPC interceptor tests
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.mockk)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic")   // a real MDC in tests; slf4j-api alone is a no-op
}
