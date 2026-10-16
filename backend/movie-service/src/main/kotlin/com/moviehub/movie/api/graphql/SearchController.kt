package com.moviehub.movie.api.graphql

import com.moviehub.movie.service.SearchResult
import com.moviehub.movie.service.SearchService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

/** GraphQL edge for search (E5-S1). No logic here. */
@Controller
class SearchController(private val searchService: SearchService) {

    @QueryMapping
    fun search(@Argument text: String): SearchResult = searchService.search(text)
}
