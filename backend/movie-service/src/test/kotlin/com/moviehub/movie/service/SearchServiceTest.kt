package com.moviehub.movie.service

import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.movie.client.PeopleClient
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.Person
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SearchServiceTest {

    private val movieService = mockk<MovieService>()
    private val peopleClient = mockk<PeopleClient>()
    private val service = SearchService(movieService, peopleClient)

    @Test
    fun `should answer movies and people for the same text`() {
        every { movieService.search("nolan") } returns listOf(Movie(1, "Nolan's Diary", 2001))
        every { peopleClient.searchPeople("nolan") } returns listOf(Person(7, "Christopher Nolan", null))

        val result = service.search(" nolan ")

        assertEquals(listOf("Nolan's Diary"), result.movies.map { it.title })
        assertEquals(listOf("Christopher Nolan"), result.people.map { it.name })
    }

    @Test
    fun `should answer empty lists for blank text without calling either side`() {
        val result = service.search("   ")

        assertEquals(0, result.movies.size + result.people.size)
        verify(exactly = 0) { movieService.search(any()) }
        verify(exactly = 0) { peopleClient.searchPeople(any()) }
    }

    @Test
    fun `should let a people-service outage escape as UNAVAILABLE`() {
        every { movieService.search("x") } returns emptyList()
        every { peopleClient.searchPeople("x") } throws DependencyUnavailableException("people-service")

        assertThrows(DependencyUnavailableException::class.java) { service.search("x") }
    }
}
