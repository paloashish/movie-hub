# contracts

The gRPC contract between the two services: `src/main/proto/moviehub/people/v1/people.proto`. The Gradle protobuf plugin generates the Java messages and the `PeopleServiceGrpc` stubs at build time; both services depend on this module and nothing else is in it.

Edit the proto, rebuild, and both sides see the change. Blocking stubs are used on purpose (ADR 0006). Field numbers are never reused.
