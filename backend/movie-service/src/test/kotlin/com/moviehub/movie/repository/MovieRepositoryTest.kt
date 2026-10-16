package com.moviehub.movie.repository

import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.MovieSort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles

/** Runs the Liquibase changeset against in-memory H2 and proves the entity matches the table. */
@DataJpaTest
@ActiveProfiles("test")
class MovieRepositoryTest(@Autowired private val repository: MovieRepository) {

    @Test
    fun `should save and read back a movie with a generated id`() {
        val saved = repository.save(Movie(title = "Inception", releaseYear = 2010, synopsis = "Dreams"))

        val found = repository.findById(requireNotNull(saved.id)).orElseThrow()

        assertEquals("Inception", found.title)
        assertEquals(2010, found.releaseYear)
        assertEquals("Dreams", found.synopsis)
        assertNull(found.posterUrl)   // no poster until E3
    }

    @Test
    fun `should sort by title ignoring case when asked`() {
        repository.saveAll(
            listOf(
                Movie(title = "zodiac", releaseYear = 2007),
                Movie(title = "Alien", releaseYear = 1979),
                Movie(title = "memento", releaseYear = 2000),
            ),
        )

        val titles = repository.findAll(Sort.by(Sort.Order.asc("title").ignoreCase())).map { it.title }

        assertEquals(listOf("Alien", "memento", "zodiac"), titles)
    }

    @Test
    fun `should find movies by a partial title regardless of case`() {
        repository.saveAll(
            listOf(
                Movie(title = "Inception", releaseYear = 2010),
                Movie(title = "Interstellar", releaseYear = 2014),
                Movie(title = "Alien", releaseYear = 1979),
            ),
        )

        val titles = repository.findByTitleContainingIgnoreCaseOrderByTitleAsc("INCEP").map { it.title }

        assertEquals(listOf("Inception"), titles)
    }

    @Test
    fun `should treat the SQL wildcards percent and underscore as plain characters when searching`() {
        repository.saveAll(listOf(Movie(title = "100% Wolf", releaseYear = 2020), Movie(title = "Alien", releaseYear = 1979)))

        assertEquals(listOf("100% Wolf"), repository.findByTitleContainingIgnoreCaseOrderByTitleAsc("%").map { it.title })
        assertEquals(emptyList<String>(), repository.findByTitleContainingIgnoreCaseOrderByTitleAsc("_").map { it.title })
    }

    @Test
    fun `should sort newest first with same-year movies A to Z when asked for YEAR_DESC`() {
        repository.saveAll(
            listOf(
                Movie(title = "Zodiac", releaseYear = 2007),
                Movie(title = "atonement", releaseYear = 2007),
                Movie(title = "Alien", releaseYear = 1979),
            ),
        )

        val titles = repository.findAll(MovieSort.YEAR_DESC.order).map { it.title }

        assertEquals(listOf("atonement", "Zodiac", "Alien"), titles)
    }
}
