package com.moviehub.movie.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

/** E6-S2: with the seed context on, an empty database gets the sample catalogue, and every credit points at a seeded person. */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.liquibase.contexts=schema,seed"])
class SeedDataTest(
    @Autowired private val movies: MovieRepository,
    @Autowired private val credits: CreditRepository,
) {

    @Test
    fun `should seed exactly 10 movies, each with a poster and at least one credit`() {
        val all = movies.findAll()

        assertEquals(10, all.size)
        all.forEach { movie ->
            assertNotNull(movie.posterUrl, "${movie.title} has no poster")
            assertTrue(credits.findByMovieIdOrderByIdAsc(requireNotNull(movie.id)).isNotEmpty(), "${movie.title} has no credits")
        }
    }

    @Test
    fun `should only credit people that the people-service seed creates`() {
        val seededPeople = (100001L..100020L)

        credits.findAll().forEach { assertTrue(it.personId in seededPeople, "credit ${it.id} points at unknown person ${it.personId}") }
    }

    @Test
    fun `should ship every seeded poster on the classpath`() {
        movies.findAll().forEach { movie ->
            val file = requireNotNull(movie.posterUrl).removePrefix("/posters/").substringBefore('?')
            assertTrue(ClassPathResource("seed-posters/$file").exists(), "$file is missing")
        }
    }
}
