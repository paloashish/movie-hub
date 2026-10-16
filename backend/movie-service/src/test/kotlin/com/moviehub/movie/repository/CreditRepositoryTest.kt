package com.moviehub.movie.repository

import com.moviehub.movie.domain.Credit
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.Role
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles

/** Runs the credit changeset against in-memory H2: the unique rule and the cascade are database facts. */
@DataJpaTest
@ActiveProfiles("test")
class CreditRepositoryTest(
    @Autowired private val credits: CreditRepository,
    @Autowired private val movies: MovieRepository,
) {

    private fun movie() = movies.save(Movie(title = "Alien", releaseYear = 1979))

    @Test
    fun `should save a credit and find it by movie`() {
        val movieId = requireNotNull(movie().id)
        credits.save(Credit(movieId = movieId, personId = 7, role = Role.ACTOR))

        val found = credits.findByMovieIdOrderByIdAsc(movieId)

        assertEquals(1, found.size)
        assertEquals(Role.ACTOR, found.single().role)
        assertTrue(credits.existsByMovieIdAndPersonIdAndRole(movieId, 7, Role.ACTOR))
    }

    @Test
    fun `should refuse the same person in the same role twice on one movie`() {
        val movieId = requireNotNull(movie().id)
        credits.saveAndFlush(Credit(movieId = movieId, personId = 7, role = Role.ACTOR))

        assertThrows(DataIntegrityViolationException::class.java) {
            credits.saveAndFlush(Credit(movieId = movieId, personId = 7, role = Role.ACTOR))
        }
    }

    @Test
    fun `should allow the same person in two roles on one movie`() {
        val movieId = requireNotNull(movie().id)
        credits.saveAndFlush(Credit(movieId = movieId, personId = 7, role = Role.ACTOR))
        credits.saveAndFlush(Credit(movieId = movieId, personId = 7, role = Role.DIRECTOR))

        assertEquals(2, credits.findByMovieIdOrderByIdAsc(movieId).size)
    }

    @Test
    fun `should drop the credits when the movie row is deleted`() {
        val saved = movie()
        val movieId = requireNotNull(saved.id)
        credits.saveAndFlush(Credit(movieId = movieId, personId = 7, role = Role.ACTOR))

        movies.delete(saved)
        movies.flush()

        assertEquals(0, credits.findByMovieIdOrderByIdAsc(movieId).size)
    }
}
