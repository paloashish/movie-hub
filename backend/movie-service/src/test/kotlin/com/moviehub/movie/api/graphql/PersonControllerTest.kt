package com.moviehub.movie.api.graphql

import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import com.moviehub.movie.client.PeopleClient
import com.moviehub.movie.service.CreditService
import com.moviehub.movie.common.GraphQlExceptionResolver
import com.moviehub.movie.domain.Person
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
import org.springframework.graphql.test.tester.GraphQlTester

@GraphQlTest(controllers = [PersonController::class])
@Import(GraphQlExceptionResolver::class, PersonControllerTest.Mocks::class)
class PersonControllerTest(
    @Autowired private val tester: GraphQlTester,
    @Autowired private val peopleClient: PeopleClient,
    @Autowired private val creditService: CreditService,
) {

    @TestConfiguration
    class Mocks {
        @Bean fun peopleClient(): PeopleClient = mockk()
        @Bean fun creditService(): CreditService = mockk()
    }

    // The mock bean lives in the shared Spring context, so recorded calls would leak between tests
    @BeforeEach
    fun resetMock() = clearMocks(peopleClient, creditService)

    @Test
    fun `should return people with id, name and bio`() {
        every { peopleClient.listPeople() } returns listOf(Person(1, "Anna", "Actor"), Person(2, "Bob", null))

        val response = tester.document("{ people { id name bio } }").execute()

        response.path("people").entityList(Person::class.java).hasSize(2)
        response.path("people[0].id").entity(String::class.java).isEqualTo("1")
        response.path("people[0].name").entity(String::class.java).isEqualTo("Anna")
        response.path("people[0].bio").entity(String::class.java).isEqualTo("Actor")
        response.path("people[1].bio").valueIsNull()
    }

    @Test
    fun `should return an empty list when there are no people`() {
        every { peopleClient.listPeople() } returns emptyList()

        tester.document("{ people { id } }").execute().path("people").entityList(Any::class.java).hasSize(0)
    }

    @Test
    fun `should return an UNAVAILABLE error when people-service is down`() {
        every { peopleClient.listPeople() } throws DependencyUnavailableException("people-service")

        tester.document("{ people { id } }").execute().errors().satisfy { errors ->
            assertEquals("UNAVAILABLE", errors.single().extensions["code"])
        }
    }

    // E1-S2

    private val createPerson = """
        mutation Create(${'$'}input: PersonInput!) { createPerson(input: ${'$'}input) { id name bio } }
    """

    @Test
    fun `should create a person and return it with its id`() {
        every { peopleClient.createPerson("Anna", "Actor") } returns Person(7, "Anna", "Actor")

        val response = tester.document(createPerson).variable("input", mapOf("name" to "Anna", "bio" to "Actor")).execute()

        response.path("createPerson.id").entity(String::class.java).isEqualTo("7")
        response.path("createPerson.name").entity(String::class.java).isEqualTo("Anna")
    }

    @Test
    fun `should return a VALIDATION error naming the field and not call people-service when the name is blank`() {
        tester.document(createPerson).variable("input", mapOf("name" to "   ")).execute().errors().satisfy { errors ->
            assertEquals("VALIDATION", errors.single().extensions["code"])
            val message = requireNotNull(errors.single().message)
            assertTrue(message.contains("name"), message)
        }
        verify(exactly = 0) { peopleClient.createPerson(any(), any()) }
    }

    @Test
    fun `should pass through a VALIDATION error raised by people-service`() {
        every { peopleClient.createPerson("Anna", null) } throws ValidationException("name already taken")

        tester.document(createPerson).variable("input", mapOf("name" to "Anna")).execute().errors().satisfy { errors ->
            assertEquals("VALIDATION", errors.single().extensions["code"])
            assertEquals("name already taken", errors.single().message)
        }
    }

    // E1-S3

    private val updatePerson = """
        mutation Update(${'$'}id: ID!, ${'$'}input: PersonInput!) { updatePerson(id: ${'$'}id, input: ${'$'}input) { id name bio } }
    """

    @Test
    fun `should return one person by id`() {
        every { peopleClient.getPerson(3) } returns Person(3, "Anna", "Actor")

        val response = tester.document("{ person(id: \"3\") { id name bio } }").execute()

        response.path("person.name").entity(String::class.java).isEqualTo("Anna")
    }

    @Test
    fun `should return a NOT_FOUND error when the person does not exist`() {
        every { peopleClient.getPerson(99) } throws NotFoundException("Person", 99)

        tester.document("{ person(id: \"99\") { id } }").execute().errors().satisfy { errors ->
            assertEquals("NOT_FOUND", errors.single().extensions["code"])
            assertEquals("Person 99 not found", errors.single().message)
        }
    }

    @Test
    fun `should return a VALIDATION error when the id is not a number`() {
        tester.document("{ person(id: \"abc\") { id } }").execute().errors().satisfy { errors ->
            assertEquals("VALIDATION", errors.single().extensions["code"])   // Spring's Long binding fails before the body runs
        }
    }

    @Test
    fun `should update a person and return the new values`() {
        every { peopleClient.updatePerson(3, "Anna B", null) } returns Person(3, "Anna B", null)

        val response = tester.document(updatePerson)
            .variable("id", "3").variable("input", mapOf("name" to "Anna B")).execute()

        response.path("updatePerson.name").entity(String::class.java).isEqualTo("Anna B")
    }

    @Test
    fun `should return a NOT_FOUND error when updating a person deleted meanwhile`() {
        every { peopleClient.updatePerson(99, "Anna", null) } throws NotFoundException("Person", 99)

        tester.document(updatePerson).variable("id", "99").variable("input", mapOf("name" to "Anna"))
            .execute().errors().satisfy { errors ->
                assertEquals("NOT_FOUND", errors.single().extensions["code"])
            }
    }

    @Test
    fun `should return a VALIDATION error and not call people-service when updating with a blank name`() {
        tester.document(updatePerson).variable("id", "3").variable("input", mapOf("name" to " "))
            .execute().errors().satisfy { errors ->
                assertEquals("VALIDATION", errors.single().extensions["code"])
            }
        verify(exactly = 0) { peopleClient.updatePerson(any(), any(), any()) }
    }

    // E1-S4

    @Test
    fun `should delete a person and return true`() {
        every { creditService.removePerson(3) } returns Unit

        tester.document("mutation { deletePerson(id: \"3\") }").execute()
            .path("deletePerson").entity(Boolean::class.java).isEqualTo(true)
        verify(exactly = 1) { creditService.removePerson(3) }   // credits first, then the person (E4-S3)
    }

    @Test
    fun `should return a VALIDATION error when deleting with an id that is not a number`() {
        tester.document("mutation { deletePerson(id: \"abc\") }").execute().errors().satisfy { errors ->
            assertEquals("VALIDATION", errors.single().extensions["code"])
        }
        verify(exactly = 0) { creditService.removePerson(any()) }
    }

    @Test
    fun `should return a NOT_FOUND error when deleting a person that is already gone`() {
        every { creditService.removePerson(99) } throws NotFoundException("Person", 99)

        tester.document("mutation { deletePerson(id: \"99\") }").execute().errors().satisfy { errors ->
            assertEquals("NOT_FOUND", errors.single().extensions["code"])
        }
    }

    // E4-S1

    @Test
    fun `should search people by text`() {
        every { peopleClient.searchPeople("nol") } returns listOf(Person(1, "Christopher Nolan", null))

        tester.document("{ searchPeople(text: \"nol\") { id name } }").execute()
            .path("searchPeople[0].name").entity(String::class.java).isEqualTo("Christopher Nolan")
    }

    @Test
    fun `should return an empty list when no one matches the search`() {
        every { peopleClient.searchPeople("zzz") } returns emptyList()

        tester.document("{ searchPeople(text: \"zzz\") { id } }").execute().path("searchPeople").entityList(Any::class.java).hasSize(0)
    }

    @Test
    fun `should return an UNAVAILABLE error when people-service is down during a delete`() {
        every { creditService.removePerson(3) } throws DependencyUnavailableException("people-service")

        tester.document("mutation { deletePerson(id: \"3\") }").execute().errors().satisfy { errors ->
            assertEquals("UNAVAILABLE", errors.single().extensions["code"])
        }
    }
}
