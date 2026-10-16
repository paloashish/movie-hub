package com.moviehub.movie.client

import com.google.protobuf.Empty
import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import com.moviehub.contracts.people.v1.CreatePersonRequest
import com.moviehub.contracts.people.v1.GetPeopleRequest
import com.moviehub.contracts.people.v1.DeletePersonRequest
import com.moviehub.contracts.people.v1.UpdatePersonRequest
import com.moviehub.contracts.people.v1.PeopleList
import com.moviehub.contracts.people.v1.PeopleServiceGrpc
import com.moviehub.contracts.people.v1.Person as PersonProto
import com.moviehub.movie.domain.Person
import io.grpc.Status
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class PeopleClientTest {

    // withDeadlineAfter returns a new stub in real gRPC; the mock returns itself so the stubbed calls below apply
    private val stub = mockk<PeopleServiceGrpc.PeopleServiceBlockingStub> {
        every { withDeadlineAfter(any(), any()) } returns this@mockk
    }
    private val client = PeopleClient(stub)

    private fun proto(id: Long, name: String, bio: String = "") =
        PersonProto.newBuilder().setId(id).setName(name).setBio(bio).build()

    @Test
    fun `should map the proto list to domain people, turning an empty bio into null`() {
        every { stub.listPeople(any()) } returns
            PeopleList.newBuilder().addPeople(proto(1, "Anna", "Actor")).addPeople(proto(2, "Bob")).build()

        val people = client.listPeople()

        assertEquals(listOf(Person(1, "Anna", "Actor"), Person(2, "Bob", null)), people)
    }

    @Test
    fun `should return an empty list when people-service has no people`() {
        every { stub.listPeople(any()) } returns PeopleList.getDefaultInstance()

        assertEquals(emptyList<Person>(), client.listPeople())
    }

    @Test
    fun `should throw DependencyUnavailableException when people-service is down`() {
        every { stub.listPeople(any()) } throws Status.UNAVAILABLE.asRuntimeException()

        assertThrows(DependencyUnavailableException::class.java) { client.listPeople() }
    }

    @Test
    fun `should throw DependencyUnavailableException when people-service does not answer within the deadline`() {
        every { stub.listPeople(any()) } throws Status.DEADLINE_EXCEEDED.asRuntimeException()

        assertThrows(DependencyUnavailableException::class.java) { client.listPeople() }
    }

    @Test
    fun `should set a deadline on every call`() {
        every { stub.listPeople(any()) } returns PeopleList.getDefaultInstance()

        client.listPeople()

        verify(exactly = 1) { stub.withDeadlineAfter(5, TimeUnit.SECONDS) }
    }

    // E1-S2

    @Test
    fun `should send name and bio and map the created person back when creating`() {
        val request = slot<CreatePersonRequest>()
        every { stub.createPerson(capture(request)) } returns proto(7, "Anna", "Actor")

        val person = client.createPerson("Anna", "Actor")

        assertEquals("Anna", request.captured.name)
        assertEquals("Actor", request.captured.bio)
        assertEquals(Person(7, "Anna", "Actor"), person)
    }

    @Test
    fun `should send an empty bio and get null back when the bio is not given`() {
        val request = slot<CreatePersonRequest>()
        every { stub.createPerson(capture(request)) } returns proto(8, "Bob")

        val person = client.createPerson("Bob", null)

        assertEquals("", request.captured.bio)
        assertEquals(Person(8, "Bob", null), person)
    }

    @Test
    fun `should throw ValidationException with the server message when people-service rejects the input`() {
        every { stub.createPerson(any()) } throws
            Status.INVALID_ARGUMENT.withDescription("name must not be blank").asRuntimeException()

        val ex = assertThrows(ValidationException::class.java) { client.createPerson("", null) }

        assertEquals("name must not be blank", ex.message)
    }

    // E1-S3

    @Test
    fun `should get a person by id`() {
        every { stub.getPerson(any()) } returns proto(3, "Anna", "Actor")

        assertEquals(Person(3, "Anna", "Actor"), client.getPerson(3))
    }

    @Test
    fun `should throw NotFoundException with the server message when the person does not exist`() {
        every { stub.getPerson(any()) } throws Status.NOT_FOUND.withDescription("Person 99 not found").asRuntimeException()

        val ex = assertThrows(NotFoundException::class.java) { client.getPerson(99) }

        assertEquals("Person 99 not found", ex.message)
    }

    @Test
    fun `should send id, name and bio when updating and map the answer back`() {
        val request = slot<UpdatePersonRequest>()
        every { stub.updatePerson(capture(request)) } returns proto(3, "Anna B", "")

        val person = client.updatePerson(3, "Anna B", null)

        assertEquals(3L, request.captured.id)
        assertEquals("", request.captured.bio)
        assertEquals(Person(3, "Anna B", null), person)
    }

    @Test
    fun `should throw NotFoundException when updating a person that no longer exists`() {
        every { stub.updatePerson(any()) } throws Status.NOT_FOUND.withDescription("Person 99 not found").asRuntimeException()

        val ex = assertThrows(NotFoundException::class.java) { client.updatePerson(99, "x", null) }

        assertEquals("Person 99 not found", ex.message)
    }

    // E1-S4

    @Test
    fun `should send the id when deleting`() {
        val request = slot<DeletePersonRequest>()
        every { stub.deletePerson(capture(request)) } returns Empty.getDefaultInstance()

        client.deletePerson(3)

        assertEquals(3L, request.captured.id)
    }

    @Test
    fun `should throw NotFoundException when deleting a person that no longer exists`() {
        every { stub.deletePerson(any()) } throws Status.NOT_FOUND.withDescription("Person 99 not found").asRuntimeException()

        assertThrows(NotFoundException::class.java) { client.deletePerson(99) }
    }

    // E4-S1

    @Test
    fun `should fetch several people by id in one call`() {
        val request = slot<GetPeopleRequest>()
        every { stub.getPeople(capture(request)) } returns PeopleList.newBuilder().addPeople(proto(1, "Anna")).addPeople(proto(2, "Bob")).build()

        val people = client.getPeople(listOf(1L, 2L))

        assertEquals(listOf(1L, 2L), request.captured.idsList)
        assertEquals(listOf("Anna", "Bob"), people.map { it.name })
    }

    @Test
    fun `should not call people-service for an empty id list`() {
        assertEquals(emptyList<Person>(), client.getPeople(emptyList()))
        verify(exactly = 0) { stub.getPeople(any()) }
    }

    @Test
    fun `should search people by text`() {
        every { stub.searchPeople(any()) } returns PeopleList.newBuilder().addPeople(proto(1, "Christopher Nolan")).build()

        assertEquals(listOf("Christopher Nolan"), client.searchPeople("nol").map { it.name })
    }
}
