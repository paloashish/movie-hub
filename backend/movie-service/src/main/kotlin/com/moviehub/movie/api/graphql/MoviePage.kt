package com.moviehub.movie.api.graphql

import com.moviehub.movie.domain.Movie
import org.springframework.data.domain.Page

/** GraphQL `MoviePage`: the slice of Spring Data's Page that the grid needs (E2-S6). */
data class MoviePage(val items: List<Movie>, val page: Int, val size: Int, val total: Int) {
    companion object {
        // The schema says Int!; a catalogue will not reach two billion movies
        fun from(page: Page<Movie>) = MoviePage(page.content, page.number, page.size, page.totalElements.toInt())
    }
}
