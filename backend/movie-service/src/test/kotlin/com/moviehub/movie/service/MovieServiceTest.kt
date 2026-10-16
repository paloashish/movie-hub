package com.moviehub.movie.service

import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.MovieSort
import com.moviehub.movie.repository.MovieRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.Optional

class MovieServiceTest {

    private val repository = mockk<MovieRepository>()
    private val service = MovieService(repository)

    @Test
    fun `should ask for the requested page sorted by title ignoring case`() {
        val pageable = slot<Pageable>()
        every { repository.findAll(capture(pageable)) } answers {
            PageImpl(listOf(Movie(1, "alien", 1979), Movie(2, "Zodiac", 2007)), firstArg(), 14)
        }

        val result = service.list(1, 12, MovieSort.TITLE_ASC)

        assertEquals(listOf("alien", "Zodiac"), result.content.map { it.title })
        assertEquals(14, result.totalElements)
        assertEquals(1, pageable.captured.pageNumber)
        assertEquals(12, pageable.captured.pageSize)
        val order = requireNotNull(pageable.captured.sort.getOrderFor("title")) { "service must sort by title" }
        assertTrue(order.isAscending)
        assertTrue(order.isIgnoreCase)
    }

    @Test
    fun `should sort newest first with title as the tie-break when asked for YEAR_DESC`() {
        val pageable = slot<Pageable>()
        every { repository.findAll(capture(pageable)) } answers { PageImpl(emptyList(), firstArg(), 0) }

        service.list(0, 12, MovieSort.YEAR_DESC)

        val orders = pageable.captured.sort.toList()
        assertEquals(listOf("releaseYear", "title"), orders.map { it.property })
        assertTrue(orders[0].isDescending)
        assertTrue(orders[1].isAscending)
    }

    @Test
    fun `should sort by title Z to A ignoring case when asked for TITLE_DESC`() {
        val pageable = slot<Pageable>()
        every { repository.findAll(capture(pageable)) } answers { PageImpl(emptyList(), firstArg(), 0) }

        service.list(0, 12, MovieSort.TITLE_DESC)

        val order = requireNotNull(pageable.captured.sort.getOrderFor("title"))
        assertTrue(order.isDescending)
        assertTrue(order.isIgnoreCase)
    }

    @Test
    fun `should return an empty page when there are no movies`() {
        every { repository.findAll(any<Pageable>()) } answers { PageImpl(emptyList(), firstArg(), 0) }

        assertEquals(0, service.list(0, 12, MovieSort.TITLE_ASC).totalElements)
    }

    @Test
    fun `should throw ValidationException when the page is negative`() {
        assertThrows(ValidationException::class.java) { service.list(-1, 12, MovieSort.TITLE_ASC) }
    }

    @Test
    fun `should throw ValidationException when the size is 0`() {
        assertThrows(ValidationException::class.java) { service.list(0, 0, MovieSort.TITLE_ASC) }
    }

    @Test
    fun `should throw ValidationException when the size is above 50`() {
        assertThrows(ValidationException::class.java) { service.list(0, 51, MovieSort.TITLE_ASC) }
    }

    @Test
    fun `should accept the size boundaries 1 and 50`() {
        every { repository.findAll(any<Pageable>()) } answers { PageImpl(emptyList(), firstArg(), 0) }

        service.list(0, 1, MovieSort.TITLE_ASC)
        service.list(0, 50, MovieSort.TITLE_ASC)

        verify(exactly = 2) { repository.findAll(any<Pageable>()) }
    }

    // E2-S2

    @Test
    fun `should return the movie when it exists`() {
        every { repository.findById(1) } returns Optional.of(Movie(1, "Alien", 1979))

        assertEquals("Alien", service.get(1).title)
    }

    @Test
    fun `should throw NotFoundException when getting a missing movie`() {
        every { repository.findById(99) } returns Optional.empty()

        val ex = assertThrows(NotFoundException::class.java) { service.get(99) }

        assertEquals("Movie 99 not found", ex.message)
    }

    // E2-S3

    @Test
    fun `should save a movie with trimmed title and synopsis when creating`() {
        val saved = slot<Movie>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        val movie = service.create("  Alien ", 1979, " Space horror ")

        assertEquals("Alien", movie.title)
        assertEquals(1979, movie.releaseYear)
        assertEquals("Space horror", movie.synopsis)
    }

    @Test
    fun `should keep a missing synopsis as null when creating`() {
        every { repository.save(any()) } answers { firstArg() }

        assertEquals(null, service.create("Alien", 1979, null).synopsis)
    }

    @Test
    fun `should store a blank synopsis as null when creating`() {
        every { repository.save(any()) } answers { firstArg() }

        assertEquals(null, service.create("Alien", 1979, "   ").synopsis)
    }

    // E2-S4

    @Test
    fun `should update title, year and synopsis, trimmed, and bump updatedAt`() {
        val existing = Movie(1, "Old", 1999, "Old synopsis")
        val before = existing.updatedAt
        every { repository.findById(1) } returns Optional.of(existing)
        every { repository.save(any()) } answers { firstArg() }

        val updated = service.update(1, " New ", 2001, "  ")

        assertEquals("New", updated.title)
        assertEquals(2001, updated.releaseYear)
        assertEquals(null, updated.synopsis)
        assertFalse(updated.updatedAt.isBefore(before))
    }

    @Test
    fun `should throw NotFoundException and save nothing when updating a missing movie`() {
        every { repository.findById(99) } returns Optional.empty()

        assertThrows(NotFoundException::class.java) { service.update(99, "x", 2000, null) }
        verify(exactly = 0) { repository.save(any()) }
    }

    // E2-S5

    @Test
    fun `should delete the movie when it exists and hand back its poster url for the file clean-up`() {
        val existing = Movie(1, "Alien", 1979, posterUrl = "/posters/1.png")
        every { repository.findById(1) } returns Optional.of(existing)
        every { repository.delete(existing) } returns Unit

        val posterUrl = service.delete(1)

        verify(exactly = 1) { repository.delete(existing) }
        assertEquals("/posters/1.png", posterUrl)
    }

    @Test
    fun `should throw NotFoundException and delete nothing when the movie is missing`() {
        every { repository.findById(99) } returns Optional.empty()

        assertThrows(NotFoundException::class.java) { service.delete(99) }
        verify(exactly = 0) { repository.delete(any()) }
    }

    // E5-S1

    @Test
    fun `should search by partial title ignoring case`() {
        every { repository.findByTitleContainingIgnoreCaseOrderByTitleAsc("incep") } returns
            listOf(Movie(1, "Inception", 2010))

        assertEquals(listOf("Inception"), service.search("  incep ").map { it.title })
    }

    @Test
    fun `should return nothing for a blank search without touching the database`() {
        assertEquals(emptyList<Movie>(), service.search("  "))
        verify(exactly = 0) { repository.findByTitleContainingIgnoreCaseOrderByTitleAsc(any()) }
    }
}
