package com.moviehub.movie.api.graphql

import com.moviehub.common.error.ConflictException
import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.common.error.NotFoundException
import com.moviehub.movie.common.GraphQlExceptionResolver
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.Person
import com.moviehub.movie.domain.Role
import com.moviehub.movie.service.CreditService
import com.moviehub.movie.service.CreditWithPerson
import com.moviehub.movie.service.PersonCredit
import com.moviehub.movie.service.MovieService
import com.moviehub.movie.service.PosterService
import com.moviehub.movie.client.PeopleClient
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester

@GraphQlTest(controllers = [CreditController::class, MovieController::class, PersonController::class])
@Import(GraphQlExceptionResolver::class, CreditControllerTest.Mocks::class)
class CreditControllerTest(
    @Autowired private val tester: GraphQlTester,
    @Autowired private val creditService: CreditService,
    @Autowired private val movieService: MovieService,
    @Autowired private val peopleClient: PeopleClient,
) {

    @TestConfiguration
    class Mocks {
        @Bean fun creditService(): CreditService = mockk()
        @Bean fun movieService(): MovieService = mockk()
        @Bean fun posterService(): PosterService = mockk(relaxed = true)
        @Bean fun peopleClient(): PeopleClient = mockk()
    }

    @BeforeEach
    fun resetMocks() = clearMocks(creditService, movieService, peopleClient)

    private val anna = CreditWithPerson(10, Role.ACTOR, Person(7, "Anna", null))

    @Test
    fun `should resolve credits with role and person on a movie`() {
        every { movieService.get(1) } returns Movie(1, "Alien", 1979)
        every { creditService.listForMovie(1) } returns listOf(anna, CreditWithPerson(11, Role.DIRECTOR, Person(8, "Chris", null)))

        val response = tester.document("{ movie(id: \"1\") { credits { id role person { id name } } } }").execute()

        response.path("movie.credits").entityList(Any::class.java).hasSize(2)
        response.path("movie.credits[0].role").entity(String::class.java).isEqualTo("ACTOR")
        response.path("movie.credits[0].person.name").entity(String::class.java).isEqualTo("Anna")
        response.path("movie.credits[1].role").entity(String::class.java).isEqualTo("DIRECTOR")
    }

    @Test
    fun `should add a credit and return it`() {
        every { creditService.add(1, 7, Role.ACTOR) } returns anna

        val response = tester.document("mutation { addCredit(movieId: \"1\", personId: \"7\", role: ACTOR) { id role person { name } } }").execute()

        response.path("addCredit.id").entity(String::class.java).isEqualTo("10")
        response.path("addCredit.person.name").entity(String::class.java).isEqualTo("Anna")
    }

    @Test
    fun `should return a CONFLICT error when the person already holds that role`() {
        every { creditService.add(1, 7, Role.ACTOR) } throws ConflictException("Anna is already credited as actor")

        tester.document("mutation { addCredit(movieId: \"1\", personId: \"7\", role: ACTOR) { id } }").execute().errors().satisfy { errors ->
            assertEquals("CONFLICT", errors.single().extensions["code"])
            assertEquals("Anna is already credited as actor", errors.single().message)
        }
    }

    @Test
    fun `should return a NOT_FOUND error when the person does not exist`() {
        every { creditService.add(1, 99, Role.WRITER) } throws NotFoundException("Person", 99)

        tester.document("mutation { addCredit(movieId: \"1\", personId: \"99\", role: WRITER) { id } }").execute().errors().satisfy { errors ->
            assertEquals("NOT_FOUND", errors.single().extensions["code"])
        }
    }

    @Test
    fun `should reject an unknown role before reaching the service`() {
        tester.document("mutation { addCredit(movieId: \"1\", personId: \"7\", role: COMPOSER) { id } }").execute().errors().satisfy { errors ->
            assertEquals("ValidationError", errors.single().errorType.toString())   // GraphQL itself: not a member of enum Role
        }
        verify(exactly = 0) { creditService.add(any(), any(), any()) }
    }

    @Test
    fun `should answer UNAVAILABLE on the credits field when people-service is down`() {
        every { movieService.get(1) } returns Movie(1, "Alien", 1979)
        every { creditService.listForMovie(1) } throws DependencyUnavailableException("people-service")

        tester.document("{ movie(id: \"1\") { title credits { id } } }").execute().errors().satisfy { errors ->
            assertEquals("UNAVAILABLE", errors.single().extensions["code"])
        }
    }

    // E4-S2

    @Test
    fun `should remove a credit and return true`() {
        every { creditService.remove(10) } returns Unit

        tester.document("mutation { removeCredit(id: \"10\") }").execute().path("removeCredit").entity(Boolean::class.java).isEqualTo(true)
    }

    @Test
    fun `should return a NOT_FOUND error when removing a credit that is already gone`() {
        every { creditService.remove(99) } throws NotFoundException("Credit", 99)

        tester.document("mutation { removeCredit(id: \"99\") }").execute().errors().satisfy { errors ->
            assertEquals("NOT_FOUND", errors.single().extensions["code"])
        }
    }

    // E4-S3

    @Test
    fun `should resolve a person's credits with role and movie`() {
        every { peopleClient.getPerson(7) } returns Person(7, "Anna", null)
        every { creditService.listForPerson(7) } returns listOf(PersonCredit(10, Role.ACTOR, Movie(1, "Alien", 1979, posterUrl = "/posters/1.jpg?v=1")))

        val response = tester.document("{ person(id: \"7\") { name credits { id role movie { id title releaseYear posterUrl } } } }").execute()

        response.path("person.credits[0].role").entity(String::class.java).isEqualTo("ACTOR")
        response.path("person.credits[0].movie.title").entity(String::class.java).isEqualTo("Alien")
        response.path("person.credits[0].movie.releaseYear").entity(Int::class.java).isEqualTo(1979)
    }

    @Test
    fun `should answer an empty credits list for a person with none`() {
        every { peopleClient.getPerson(7) } returns Person(7, "Anna", null)
        every { creditService.listForPerson(7) } returns emptyList()

        tester.document("{ person(id: \"7\") { credits { id } } }").execute().path("person.credits").entityList(Any::class.java).hasSize(0)
    }
}
