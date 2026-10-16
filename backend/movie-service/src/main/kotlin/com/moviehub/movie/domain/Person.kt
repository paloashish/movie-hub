package com.moviehub.movie.domain

/**
 * movie-service's view of a person. It is a copy of what people-service returned, not an entity:
 * people live in the other database (ADR 0001). Proto types never leave PeopleClient.
 */
data class Person(
    val id: Long,
    val name: String,
    val bio: String?,
)
