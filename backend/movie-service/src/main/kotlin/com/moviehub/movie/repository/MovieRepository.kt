package com.moviehub.movie.repository

import com.moviehub.movie.domain.Movie
import org.springframework.data.jpa.repository.JpaRepository

interface MovieRepository : JpaRepository<Movie, Long> {
    /** E5-S1 search (ADR 0005): case-insensitive partial match on the title. */
    fun findByTitleContainingIgnoreCaseOrderByTitleAsc(text: String): List<Movie>
}
