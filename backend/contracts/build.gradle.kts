// Shared gRPC contract: the .proto file lives here and the Java stubs are generated at build time.
// Both services depend on this module; nothing else in the repo touches protobuf tooling.
plugins {
    `java-library`
    alias(libs.plugins.protobuf)
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

dependencies {
    api(platform(libs.spring.boot.bom))            // Boot-managed versions for grpc-* and protobuf-java
    api("io.grpc:grpc-protobuf")
    api("io.grpc:grpc-stub")
    api("com.google.protobuf:protobuf-java")
    compileOnly("jakarta.annotation:jakarta.annotation-api") // @Generated on generated classes
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:${libs.versions.protoc.get()}" }
    plugins {
        create("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpcGen.get()}" }
    }
    generateProtoTasks {
        all().forEach { task -> task.plugins { create("grpc") } }
    }
}
