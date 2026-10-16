package com.moviehub.people

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/** Proves the application context, Liquibase and the gRPC server all start together. */
@SpringBootTest
@ActiveProfiles("test")
class PeopleServiceApplicationTest {

    @Test
    fun `should start the application context with the test profile`() {
        // Reaching this line means startup succeeded.
    }
}
