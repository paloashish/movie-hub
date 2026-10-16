package com.moviehub.movie

import com.moviehub.contracts.people.v1.PeopleServiceGrpc
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.grpc.client.ImportGrpcClients

@SpringBootApplication
// Creates the people-service blocking stub as a bean, bound to the channel named "people" (ADR 0006).
@ImportGrpcClients(target = "people", types = [PeopleServiceGrpc.PeopleServiceBlockingStub::class])
class MovieServiceApplication

fun main(args: Array<String>) {
    runApplication<MovieServiceApplication>(*args)
}
