package com.moviehub.movie.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles

/**
 * E6-S2 guard, not tied to a production class: it exists because every repository test asserts exact lists
 * and would silently break if the seed ever ran under the test profile. If this fails, check
 * `spring.liquibase.contexts` in the test profile before anything else.
 */
@DataJpaTest
@ActiveProfiles("test")
class NoSeedInTestsTest(@Autowired private val movies: MovieRepository) {

    @Test
    fun `should start empty under the test profile`() {
        assertEquals(0, movies.count())
    }
}
