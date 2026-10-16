package com.moviehub.movie.repository

import com.moviehub.movie.domain.Credit
import com.moviehub.movie.domain.Role
import org.springframework.data.jpa.repository.JpaRepository

interface CreditRepository : JpaRepository<Credit, Long> {
    fun findByMovieIdOrderByIdAsc(movieId: Long): List<Credit>
    fun existsByMovieIdAndPersonIdAndRole(movieId: Long, personId: Long, role: Role): Boolean
    fun findByPersonId(personId: Long): List<Credit>
    fun deleteByPersonId(personId: Long): Long
}
