package com.moviehub.movie.api.graphql

import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import com.moviehub.movie.common.GraphQlExceptionResolver
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.MovieSort
import com.moviehub.movie.service.MovieService
import com.moviehub.movie.service.PosterService
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.test.tester.GraphQlTester
import java.time.Year

@GraphQlTest(controllers = [MovieController::class])
@Import(GraphQlExceptionResolver::class, MovieControllerTest.Mocks::class)
class MovieControllerTest(
    @Autowired private val tester: GraphQlTester,
    @Autowired private val movieService: MovieService,
    @Autowired private val posterService: PosterService,
) {

    @TestConfiguration
    class Mocks {
        @Bean fun movieService(): MovieService = mockk()
        @Bean fun posterService(): PosterService = mockk(relaxed = true)
    }

    // The mock beans live in the shared Spring context, so recorded calls would leak between tests
    @BeforeEach
    fun resetMocks() = clearMocks(movieService, posterService)

    private fun pageOf(vararg movies: Movie, page: Int = 0, size: Int = 12, total: Long = movies.size.toLong()) =
        PageImpl(movies.toList(), PageRequest.of(page, size), total)

    @Test
    fun `should return a page of movies with id, year, synopsis and posterUrl, null when absent`() {
        every { movieService.list(0, 12, MovieSort.TITLE_ASC) } returns pageOf(Movie(1, "Alien", 1979, "Space", "/posters/1.jpg"), Movie(2, "Memento", 2000))

        val response = tester.document("{ movies { items { id title releaseYear synopsis posterUrl } page size total } }").execute()

        response.path("movies.items").entityList(Any::class.java).hasSize(2)
        response.path("movies.items[0].id").entity(String::class.java).isEqualTo("1")
        response.path("movies.items[0].releaseYear").entity(Int::class.java).isEqualTo(1979)
        response.path("movies.items[0].posterUrl").entity(String::class.java).isEqualTo("/posters/1.jpg")
        response.path("movies.items[1].posterUrl").valueIsNull()
        response.path("movies.items[1].synopsis").valueIsNull()
        response.path("movies.total").entity(Int::class.java).isEqualTo(2)
    }

    @Test
    fun `should use page 0 and size 12 when the client sends no arguments`() {
        every { movieService.list(0, 12, MovieSort.TITLE_ASC) } returns pageOf()

        val response = tester.document("{ movies { page size total } }").execute()

        response.path("movies.page").entity(Int::class.java).isEqualTo(0)
        response.path("movies.size").entity(Int::class.java).isEqualTo(12)
        response.path("movies.total").entity(Int::class.java).isEqualTo(0)
    }

    @Test
    fun `should pass the requested page and size to the service and echo them back with the total`() {
        // total 20 with offset 10 + size 5: PageImpl keeps a total that reaches past the page as given
        every { movieService.list(2, 5, MovieSort.TITLE_ASC) } returns pageOf(Movie(13, "M", 2000), page = 2, size = 5, total = 20)

        val response = tester.document("{ movies(page: 2, size: 5) { items { id } page size total } }").execute()

        response.path("movies.page").entity(Int::class.java).isEqualTo(2)
        response.path("movies.size").entity(Int::class.java).isEqualTo(5)
        response.path("movies.total").entity(Int::class.java).isEqualTo(20)
    }

    @Test
    fun `should answer with an error code and not call the service when page is sent as null`() {
        // Only reachable from GraphiQL: the frontend never sends null. Spring cannot bind null to Int.
        tester.document("{ movies(page: null) { page } }").execute().errors().satisfy { errors ->
            assertTrue(errors.single().extensions["code"] in setOf("VALIDATION", "INTERNAL"))
        }
        verify(exactly = 0) { movieService.list(any(), any(), any()) }
    }

    @Test
    fun `should pass the chosen sort to the service`() {
        every { movieService.list(0, 12, MovieSort.YEAR_DESC) } returns pageOf()

        tester.document("{ movies(sort: YEAR_DESC) { total } }").execute().path("movies.total").entity(Int::class.java).isEqualTo(0)
    }

    @Test
    fun `should reject an unknown sort before reaching the service`() {
        tester.document("{ movies(sort: RATING) { total } }").execute().errors().satisfy { errors ->
            assertEquals("ValidationError", errors.single().errorType.toString())
        }
        verify(exactly = 0) { movieService.list(any(), any(), any()) }
    }

    @Test
    fun `should return a VALIDATION error for a negative page`() {
        every { movieService.list(-1, 12, MovieSort.TITLE_ASC) } throws ValidationException("page must be 0 or more")

        tester.document("{ movies(page: -1) { page } }").execute().errors().satisfy { errors ->
            assertEquals("VALIDATION", errors.single().extensions["code"])
        }
    }

    @Test
    fun `should hide an unexpected failure behind INTERNAL`() {
        every { movieService.list(0, 12, MovieSort.TITLE_ASC) } throws IllegalStateException("db gone")

        tester.document("{ movies { page } }").execute().errors().satisfy { errors ->
            assertEquals("INTERNAL", errors.single().extensions["code"])
            assertEquals("Something went wrong", errors.single().message)
        }
    }

    // E2-S2

    @Test
    fun `should return one movie by id`() {
        every { movieService.get(3) } returns Movie(3, "Alien", 1979, "Space", "/posters/3.jpg")

        val response = tester.document("{ movie(id: \"3\") { id title releaseYear synopsis posterUrl } }").execute()

        response.path("movie.title").entity(String::class.java).isEqualTo("Alien")
        response.path("movie.posterUrl").entity(String::class.java).isEqualTo("/posters/3.jpg")
    }

    @Test
    fun `should return a NOT_FOUND error when the movie does not exist`() {
        every { movieService.get(99) } throws NotFoundException("Movie", 99)

        tester.document("{ movie(id: \"99\") { id } }").execute().errors().satisfy { errors ->
            assertEquals("NOT_FOUND", errors.single().extensions["code"])
            assertEquals("Movie 99 not found", errors.single().message)
        }
    }

    @Test
    fun `should return a VALIDATION error when the id is not a number`() {
        tester.document("{ movie(id: \"abc\") { id } }").execute().errors().satisfy { errors ->
            assertEquals("VALIDATION", errors.single().extensions["code"])
        }
    }

    // E2-S3

    private val createMovie = """
        mutation Create(${'$'}input: MovieInput!) { createMovie(input: ${'$'}input) { id title releaseYear } }
    """

    private fun createErrors(input: Map<String, Any?>) =
        tester.document(createMovie).variable("input", input).execute().errors()

    @Test
    fun `should create a movie and return it with its id`() {
        every { movieService.create("Alien", 1979, null) } returns Movie(7, "Alien", 1979)

        val response = tester.document(createMovie).variable("input", mapOf("title" to "Alien", "releaseYear" to 1979)).execute()

        response.path("createMovie.id").entity(String::class.java).isEqualTo("7")
        response.path("createMovie.title").entity(String::class.java).isEqualTo("Alien")
    }

    @Test
    fun `should return a VALIDATION error naming title and not save when the title is blank`() {
        createErrors(mapOf("title" to "  ", "releaseYear" to 1979)).satisfy { errors ->
            assertEquals("VALIDATION", errors.single().extensions["code"])
            val message = requireNotNull(errors.single().message)
            assertTrue(message.contains("title"), message)
        }
        verify(exactly = 0) { movieService.create(any(), any(), any()) }
    }

    @Test
    fun `should return a VALIDATION error naming title when the title is longer than 200 characters`() {
        createErrors(mapOf("title" to "a".repeat(201), "releaseYear" to 1979)).satisfy { errors ->
            assertEquals("VALIDATION", errors.single().extensions["code"])
            assertTrue(requireNotNull(errors.single().message).contains("title"))
        }
        verify(exactly = 0) { movieService.create(any(), any(), any()) }
    }

    @Test
    fun `should return a VALIDATION error when the year is before 1888`() {
        createErrors(mapOf("title" to "Alien", "releaseYear" to 1887)).satisfy { errors ->
            assertEquals("VALIDATION", errors.single().extensions["code"])
            assertTrue(requireNotNull(errors.single().message).contains("1888"))
        }
    }

    @Test
    fun `should return a VALIDATION error when the year is more than five years ahead`() {
        createErrors(mapOf("title" to "Alien", "releaseYear" to Year.now().value + 6)).satisfy { errors ->
            assertEquals("VALIDATION", errors.single().extensions["code"])
            assertTrue(requireNotNull(errors.single().message).contains("future"))
        }
    }

    @Test
    fun `should accept a year exactly five years ahead`() {
        val year = Year.now().value + 5
        every { movieService.create("Soon", year, null) } returns Movie(8, "Soon", year)

        tester.document(createMovie).variable("input", mapOf("title" to "Soon", "releaseYear" to year)).execute()
            .path("createMovie.releaseYear").entity(Int::class.java).isEqualTo(year)
    }

    // E2-S4

    private val updateMovie = """
        mutation Update(${'$'}id: ID!, ${'$'}input: MovieInput!) { updateMovie(id: ${'$'}id, input: ${'$'}input) { id title releaseYear } }
    """

    @Test
    fun `should update a movie and return the new values`() {
        every { movieService.update(3, "Aliens", 1986, null) } returns Movie(3, "Aliens", 1986)

        tester.document(updateMovie).variable("id", "3").variable("input", mapOf("title" to "Aliens", "releaseYear" to 1986))
            .execute().path("updateMovie.title").entity(String::class.java).isEqualTo("Aliens")
    }

    @Test
    fun `should return a NOT_FOUND error when updating a movie deleted meanwhile`() {
        every { movieService.update(99, "x", 2000, null) } throws NotFoundException("Movie", 99)

        tester.document(updateMovie).variable("id", "99").variable("input", mapOf("title" to "x", "releaseYear" to 2000))
            .execute().errors().satisfy { errors ->
                assertEquals("NOT_FOUND", errors.single().extensions["code"])
            }
    }

    @Test
    fun `should return a VALIDATION error and not call the service when updating with a bad year`() {
        tester.document(updateMovie).variable("id", "3").variable("input", mapOf("title" to "x", "releaseYear" to 1700))
            .execute().errors().satisfy { errors ->
                assertEquals("VALIDATION", errors.single().extensions["code"])
            }
        verify(exactly = 0) { movieService.update(any(), any(), any(), any()) }
    }

    // E2-S5

    @Test
    fun `should delete a movie and return true`() {
        every { movieService.delete(3) } returns null

        tester.document("mutation { deleteMovie(id: \"3\") }").execute()
            .path("deleteMovie").entity(Boolean::class.java).isEqualTo(true)
        verify(exactly = 0) { posterService.deleteFile(any()) }
    }

    @Test
    fun `should remove the poster file after deleting a movie that had one`() {
        every { movieService.delete(3) } returns "/posters/3.png"

        tester.document("mutation { deleteMovie(id: \"3\") }").execute().path("deleteMovie").entity(Boolean::class.java).isEqualTo(true)

        verify(exactly = 1) { posterService.deleteFile("/posters/3.png") }
    }

    @Test
    fun `should return a NOT_FOUND error when deleting a movie that is already gone`() {
        every { movieService.delete(99) } throws NotFoundException("Movie", 99)

        tester.document("mutation { deleteMovie(id: \"99\") }").execute().errors().satisfy { errors ->
            assertEquals("NOT_FOUND", errors.single().extensions["code"])
        }
    }

    @Test
    fun `should return a VALIDATION error when deleting with an id that is not a number`() {
        tester.document("mutation { deleteMovie(id: \"abc\") }").execute().errors().satisfy { errors ->
            assertEquals("VALIDATION", errors.single().extensions["code"])
        }
        verify(exactly = 0) { movieService.delete(any()) }
    }
}
