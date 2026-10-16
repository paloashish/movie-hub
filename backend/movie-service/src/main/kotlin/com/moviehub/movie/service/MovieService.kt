package com.moviehub.movie.service

import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.MovieSort
import com.moviehub.movie.repository.MovieRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MovieService(private val repository: MovieRepository) {

    /**
     * E2-S1 + E2-S6 + E6-S1: one page of movies in the chosen order (title A to Z by default).
     * Spring Data's Page carries the total, which the grid needs for its page controls.
     */
    fun list(page: Int, size: Int, sort: MovieSort): Page<Movie> {
        if (page < 0) throw ValidationException("page must be 0 or more")
        if (size !in 1..MAX_PAGE_SIZE) throw ValidationException("size must be between 1 and $MAX_PAGE_SIZE")
        return repository.findAll(PageRequest.of(page, size, sort.order))
    }

    /** E2-S2: one movie, or NotFoundException (GraphQL code NOT_FOUND, shown as "Movie not found"). */
    fun get(id: Long): Movie = repository.findById(id).orElseThrow { NotFoundException("Movie", id) }

    /** E2-S3: create a movie. The GraphQL edge has validated the input; this only tidies and saves. */
    @Transactional
    fun create(title: String, releaseYear: Int, synopsis: String?): Movie =
        repository.save(Movie(title = title.trim(), releaseYear = releaseYear, synopsis = synopsis?.trim()?.ifEmpty { null }))

    /** E2-S4: same tidying as create; a movie deleted meanwhile answers NOT_FOUND. */
    @Transactional
    fun update(id: Long, title: String, releaseYear: Int, synopsis: String?): Movie {
        val movie = get(id)
        movie.title = title.trim()
        movie.releaseYear = releaseYear
        movie.synopsis = synopsis?.trim()?.ifEmpty { null }
        movie.updatedAt = LocalDateTime.now()
        return repository.save(movie)
    }

    /**
     * E2-S5: delete a movie; a missing id answers NOT_FOUND. Credits join this transaction in E4.
     * Returns the poster URL the caller should remove from disk once the row is gone (E3-S1): a file delete
     * cannot roll back with the database, so it happens after this method, not inside it.
     */
    @Transactional
    fun delete(id: Long): String? {
        val movie = get(id)
        repository.delete(movie)
        return movie.posterUrl
    }

    /** E5-S1: partial, case-insensitive match on the title; blank text matches nothing (ADR 0005). */
    fun search(text: String): List<Movie> {
        val clean = text.trim()
        return if (clean.isEmpty()) emptyList() else repository.findByTitleContainingIgnoreCaseOrderByTitleAsc(clean)
    }

    private companion object {
        const val MAX_PAGE_SIZE = 50   // the UI asks for 12; this caps what GraphiQL can ask for
    }
}
