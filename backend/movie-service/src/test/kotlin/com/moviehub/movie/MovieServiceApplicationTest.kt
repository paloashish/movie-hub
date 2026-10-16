package com.moviehub.movie

import com.moviehub.movie.client.PeopleClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/** Proves the application context, Liquibase, the GraphQL schema and the gRPC client stub all load together. */
@SpringBootTest
@ActiveProfiles("test")
class MovieServiceApplicationTest(@Autowired private val peopleClient: PeopleClient) {

    @Test
    fun `should start the application context with the test profile and wire the people gRPC client`() {
        assertNotNull(peopleClient)
    }
}
