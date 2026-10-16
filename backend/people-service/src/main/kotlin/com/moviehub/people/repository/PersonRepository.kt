package com.moviehub.people.repository

import com.moviehub.people.domain.Person
import org.springframework.data.jpa.repository.JpaRepository

interface PersonRepository : JpaRepository<Person, Long> {
    /** E4-S1 person picker (and E5-S1 search): case-insensitive partial match on the name. */
    fun findByNameContainingIgnoreCaseOrderByNameAsc(text: String): List<Person>
}
