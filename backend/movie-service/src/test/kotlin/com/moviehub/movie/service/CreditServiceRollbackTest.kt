package com.moviehub.movie.service

import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.movie.client.PeopleClient
import com.moviehub.movie.domain.Credit
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.Role
import com.moviehub.movie.repository.CreditRepository
import com.moviehub.movie.repository.MovieRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * System-design 3.3 proven against a real database: deleting a person removes their credits first, then calls
 * people-service; if that call fails, the credit rows must still be there afterwards.
 * The test does not wrap itself in a transaction (NOT_SUPPORTED), so the service's own @Transactional is the one
 * that commits or rolls back, which is the behaviour under test.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(CreditService::class, CreditServiceRollbackTest.Mocks::class)
class CreditServiceRollbackTest(
    @Autowired private val credits: CreditRepository,
    @Autowired private val movies: MovieRepository,
    @Autowired private val service: CreditService,
    @Autowired private val peopleClient: PeopleClient,
) {

    @TestConfiguration
    class Mocks {
        @Bean fun peopleClient(): PeopleClient = mockk()
        @Bean fun movieService(): MovieService = mockk()
    }

    @AfterEach
    fun cleanUp() {
        credits.deleteAll()
        movies.deleteAll()
    }

    private fun creditPerson7Twice() {
        val movie = movies.save(Movie(title = "Alien", releaseYear = 1979))
        val movieId = requireNotNull(movie.id)
        credits.save(Credit(movieId = movieId, personId = 7, role = Role.ACTOR))
        credits.save(Credit(movieId = movieId, personId = 7, role = Role.WRITER))
    }

    @Test
    fun `should keep the credits when people-service fails to delete the person`() {
        creditPerson7Twice()
        every { peopleClient.deletePerson(7) } throws DependencyUnavailableException("people-service")

        assertThrows(DependencyUnavailableException::class.java) { service.removePerson(7) }

        assertEquals(2, credits.findByPersonId(7).size)   // rolled back: nothing changed
    }

    @Test
    fun `should remove the credits when people-service deletes the person`() {
        creditPerson7Twice()
        every { peopleClient.deletePerson(7) } returns Unit

        service.removePerson(7)

        assertEquals(0, credits.findByPersonId(7).size)
    }
}
