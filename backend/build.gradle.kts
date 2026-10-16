// Root build: declares plugins once (apply false) so modules pick what they need,
// and applies the few settings every module shares.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.protobuf) apply false
}

allprojects {
    group = "com.moviehub"
    version = "0.1.0"
    repositories { mavenCentral() }
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging { events("failed"); showStandardStreams = false }
    }
}
