package com.moviehub.movie.api.graphql

import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.movie.common.GraphQlExceptionResolver
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.Person
import com.moviehub.movie.service.SearchResult
import com.moviehub.movie.service.SearchService
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester

@GraphQlTest(controllers = [SearchController::class])
@Import(GraphQlExceptionResolver::class, SearchControllerTest.Mocks::class)
class SearchControllerTest(
    @Autowired private val tester: GraphQlTester,
    @Autowired private val searchService: SearchService,
) {

    @TestConfiguration
    class Mocks {
        @Bean fun searchService(): SearchService = mockk()
    }

    @BeforeEach
    fun resetMock() = clearMocks(searchService)

    @Test
    fun `should return movies and people in two groups`() {
        every { searchService.search("incep") } returns SearchResult(
            movies = listOf(Movie(1, "Inception", 2010)),
            people = listOf(Person(7, "Incepta", null)),
        )

        val response = tester.document("{ search(text: \"incep\") { movies { id title } people { id name } } }").execute()

        response.path("search.movies[0].title").entity(String::class.java).isEqualTo("Inception")
        response.path("search.people[0].name").entity(String::class.java).isEqualTo("Incepta")
    }

    @Test
    fun `should return two empty groups when nothing matches`() {
        every { searchService.search("zzz") } returns SearchResult(emptyList(), emptyList())

        val response = tester.document("{ search(text: \"zzz\") { movies { id } people { id } } }").execute()

        response.path("search.movies").entityList(Any::class.java).hasSize(0)
        response.path("search.people").entityList(Any::class.java).hasSize(0)
    }

    @Test
    fun `should return an UNAVAILABLE error when people-service is down`() {
        every { searchService.search("x") } throws DependencyUnavailableException("people-service")

        tester.document("{ search(text: \"x\") { movies { id } } }").execute().errors().satisfy { errors ->
            assertEquals("UNAVAILABLE", errors.single().extensions["code"])
        }
    }
}
