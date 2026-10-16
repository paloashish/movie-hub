package com.moviehub.movie.service

import com.moviehub.common.error.ConflictException
import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.common.error.NotFoundException
import com.moviehub.movie.client.PeopleClient
import com.moviehub.movie.domain.Credit
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.Person
import com.moviehub.movie.domain.Role
import com.moviehub.movie.repository.CreditRepository
import com.moviehub.movie.repository.MovieRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.util.Optional

class CreditServiceTest {

    private val credits = mockk<CreditRepository>()
    private val movies = mockk<MovieRepository>()
    private val movieService = mockk<MovieService>()
    private val peopleClient = mockk<PeopleClient>()
    private val service = CreditService(credits, movies, movieService, peopleClient)

    @Test
    fun `should resolve the people behind a movie's credits in one call`() {
        every { credits.findByMovieIdOrderByIdAsc(1) } returns listOf(Credit(10, 1, 7, Role.ACTOR), Credit(11, 1, 8, Role.DIRECTOR))
        every { peopleClient.getPeople(listOf(7L, 8L)) } returns listOf(Person(7, "Anna", null), Person(8, "Chris", null))

        val result = service.listForMovie(1)

        assertEquals(listOf("Anna", "Chris"), result.map { it.person.name })
        assertEquals(listOf(Role.ACTOR, Role.DIRECTOR), result.map { it.role })
    }

    @Test
    fun `should skip a credit whose person no longer exists`() {
        every { credits.findByMovieIdOrderByIdAsc(1) } returns listOf(Credit(10, 1, 7, Role.ACTOR), Credit(11, 1, 99, Role.WRITER))
        every { peopleClient.getPeople(listOf(7L, 99L)) } returns listOf(Person(7, "Anna", null))

        assertEquals(listOf(10L), service.listForMovie(1).map { it.id })
    }

    @Test
    fun `should return an empty list when the movie has no credits`() {
        every { credits.findByMovieIdOrderByIdAsc(1) } returns emptyList()
        every { peopleClient.getPeople(emptyList()) } returns emptyList()

        assertEquals(emptyList<CreditWithPerson>(), service.listForMovie(1))
    }

    @Test
    fun `should add a credit and answer it with the person`() {
        every { movieService.get(1) } returns Movie(1, "Alien", 1979)
        every { peopleClient.getPerson(7) } returns Person(7, "Anna", null)
        every { credits.existsByMovieIdAndPersonIdAndRole(1, 7, Role.ACTOR) } returns false
        every { credits.saveAndFlush(any()) } answers { Credit(10, 1, 7, Role.ACTOR) }

        val result = service.add(1, 7, Role.ACTOR)

        assertEquals(10L, result.id)
        assertEquals("Anna", result.person.name)
    }

    @Test
    fun `should throw ConflictException when the person already holds that role on the movie`() {
        every { movieService.get(1) } returns Movie(1, "Alien", 1979)
        every { peopleClient.getPerson(7) } returns Person(7, "Anna", null)
        every { credits.existsByMovieIdAndPersonIdAndRole(1, 7, Role.ACTOR) } returns true

        val ex = assertThrows(ConflictException::class.java) { service.add(1, 7, Role.ACTOR) }

        assertEquals("Anna is already credited as actor", ex.message)
        verify(exactly = 0) { credits.saveAndFlush(any()) }
    }

    @Test
    fun `should throw NotFoundException and save nothing when the movie does not exist`() {
        every { movieService.get(99) } throws NotFoundException("Movie", 99)

        assertThrows(NotFoundException::class.java) { service.add(99, 7, Role.ACTOR) }
        verify(exactly = 0) { credits.saveAndFlush(any()) }
    }

    @Test
    fun `should throw NotFoundException and save nothing when the person does not exist`() {
        every { movieService.get(1) } returns Movie(1, "Alien", 1979)
        every { peopleClient.getPerson(99) } throws NotFoundException("Person", 99)

        assertThrows(NotFoundException::class.java) { service.add(1, 99, Role.ACTOR) }
        verify(exactly = 0) { credits.saveAndFlush(any()) }
    }

    // E4-S2

    @Test
    fun `should remove the credit and leave the person alone`() {
        val credit = Credit(10, 1, 7, Role.ACTOR)
        every { credits.findById(10) } returns Optional.of(credit)
        every { credits.delete(credit) } returns Unit

        service.remove(10)

        verify(exactly = 1) { credits.delete(credit) }
    }

    @Test
    fun `should throw NotFoundException when the credit is already gone`() {
        every { credits.findById(99) } returns Optional.empty()

        assertThrows(NotFoundException::class.java) { service.remove(99) }
        verify(exactly = 0) { credits.delete(any()) }
    }

    // E4-S1 review: cross-service outage and the unique-constraint race

    @Test
    fun `should let DependencyUnavailableException escape and save nothing when people-service is down`() {
        every { movieService.get(1) } returns Movie(1, "Alien", 1979)
        every { peopleClient.getPerson(7) } throws DependencyUnavailableException("people-service")

        assertThrows(DependencyUnavailableException::class.java) { service.add(1, 7, Role.ACTOR) }
        verify(exactly = 0) { credits.saveAndFlush(any()) }
    }

    @Test
    fun `should answer CONFLICT when a concurrent duplicate trips the unique constraint`() {
        every { movieService.get(1) } returns Movie(1, "Alien", 1979)
        every { peopleClient.getPerson(7) } returns Person(7, "Anna", null)
        every { credits.existsByMovieIdAndPersonIdAndRole(1, 7, Role.ACTOR) } returns false
        every { credits.saveAndFlush(any()) } throws DataIntegrityViolationException("uq_credit_movie_person_role")

        assertThrows(ConflictException::class.java) { service.add(1, 7, Role.ACTOR) }
    }

    // E4-S3

    @Test
    fun `should list a person's movies with their role, newest first`() {
        every { credits.findByPersonId(7) } returns listOf(Credit(10, 1, 7, Role.ACTOR), Credit(11, 2, 7, Role.DIRECTOR))
        every { movies.findAllById(listOf(1L, 2L)) } returns listOf(Movie(1, "Alien", 1979), Movie(2, "Aliens", 1986))

        val result = service.listForPerson(7)

        assertEquals(listOf("Aliens", "Alien"), result.map { it.movie.title })
        assertEquals(listOf(Role.DIRECTOR, Role.ACTOR), result.map { it.role })
    }

    @Test
    fun `should order same-year movies by title`() {
        every { credits.findByPersonId(7) } returns listOf(Credit(10, 1, 7, Role.ACTOR), Credit(11, 2, 7, Role.ACTOR))
        every { movies.findAllById(listOf(1L, 2L)) } returns listOf(Movie(1, "Zodiac", 2007), Movie(2, "atonement", 2007))

        assertEquals(listOf("atonement", "Zodiac"), service.listForPerson(7).map { it.movie.title })
    }

    @Test
    fun `should return an empty list for a person with no credits`() {
        every { credits.findByPersonId(7) } returns emptyList()
        every { movies.findAllById(emptyList()) } returns emptyList()

        assertEquals(emptyList<PersonCredit>(), service.listForPerson(7))
    }

    @Test
    fun `should delete the credits before the person when removing a person`() {
        every { credits.deleteByPersonId(7) } returns 2
        every { peopleClient.deletePerson(7) } returns Unit

        service.removePerson(7)

        verifyOrder {
            credits.deleteByPersonId(7)
            peopleClient.deletePerson(7)
        }
    }

    @Test
    fun `should let a people-service failure escape so the transaction rolls the credit delete back`() {
        every { credits.deleteByPersonId(7) } returns 2
        every { peopleClient.deletePerson(7) } throws DependencyUnavailableException("people-service")

        assertThrows(DependencyUnavailableException::class.java) { service.removePerson(7) }
    }
}
