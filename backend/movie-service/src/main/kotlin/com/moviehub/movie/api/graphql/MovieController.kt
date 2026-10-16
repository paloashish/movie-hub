package com.moviehub.movie.api.graphql

import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.MovieSort
import com.moviehub.movie.service.MovieService
import com.moviehub.movie.service.PosterService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

/** GraphQL edge for movies. Maps queries to MovieService calls; no logic here (coding-standards.md). */
@Controller
class MovieController(private val movieService: MovieService, private val posterService: PosterService) {

    /** E2-S1 + E2-S6 + E6-S1. The schema supplies the defaults (page 0, size 12, TITLE_ASC) when the client sends none. */
    @QueryMapping
    fun movies(@Argument page: Int, @Argument size: Int, @Argument sort: MovieSort): MoviePage =
        MoviePage.from(movieService.list(page, size, sort))

    /** E2-S2. The ID scalar arrives as a string; Spring converts it to Long before the body runs. */
    @QueryMapping
    fun movie(@Argument id: Long): Movie = movieService.get(id)

    /** E2-S3. `@Valid` makes Spring run the MovieInput constraints before this body executes. */
    @MutationMapping
    fun createMovie(@Argument @Valid input: MovieInput): Movie =
        movieService.create(input.title, input.releaseYear, input.synopsis)

    /** E2-S4. Same `@Valid` run as create; a NOT_FOUND comes from the service when the movie is gone. */
    @MutationMapping
    fun updateMovie(@Argument id: Long, @Argument @Valid input: MovieInput): Movie =
        movieService.update(id, input.title, input.releaseYear, input.synopsis)

    /** E2-S5. Returns true because GraphQL needs a value and the movie is gone. The file goes after the row (E3-S1). */
    @MutationMapping
    fun deleteMovie(@Argument id: Long): Boolean {
        movieService.delete(id)?.let { posterService.deleteFile(it) }
        return true
    }
}
