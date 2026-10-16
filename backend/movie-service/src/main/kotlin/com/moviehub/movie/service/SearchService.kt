package com.moviehub.movie.service

import com.moviehub.movie.client.PeopleClient
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.Person
import org.springframework.stereotype.Service

/** One search answer for the frontend: movies from this database, people from people-service (ADR 0002, 0005). */
data class SearchResult(val movies: List<Movie>, val people: List<Person>)

@Service
class SearchService(private val movieService: MovieService, private val peopleClient: PeopleClient) {

    /**
     * E5-S1. A blank text answers empty lists without calling anyone.
     * Each service trims its own input too (MovieService, PersonService): every door guards itself.
     */
    fun search(text: String): SearchResult {
        val clean = text.trim()
        if (clean.isEmpty()) return SearchResult(emptyList(), emptyList())
        return SearchResult(movies = movieService.search(clean), people = peopleClient.searchPeople(clean))
    }
}
