package com.moviehub.people.grpc

import com.google.protobuf.Empty
import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import com.moviehub.contracts.people.v1.CreatePersonRequest
import com.moviehub.contracts.people.v1.DeletePersonRequest
import com.moviehub.contracts.people.v1.GetPeopleRequest
import com.moviehub.contracts.people.v1.GetPersonRequest
import com.moviehub.contracts.people.v1.ListPeopleRequest
import com.moviehub.contracts.people.v1.PeopleList
import com.moviehub.contracts.people.v1.SearchPeopleRequest
import com.moviehub.contracts.people.v1.Person as PersonProto
import com.moviehub.contracts.people.v1.UpdatePersonRequest
import com.moviehub.people.domain.Person
import com.moviehub.people.service.PersonService
import io.grpc.stub.StreamObserver
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PeopleGrpcServiceTest {

    private val personService = mockk<PersonService>()
    private val grpc = PeopleGrpcService(personService)
    private val observer = mockk<StreamObserver<PeopleList>>(relaxed = true)

    @Test
    fun `should map every person to proto and complete the call when listing people`() {
        every { personService.list() } returns listOf(Person(1, "Anna", "Actor"), Person(2, "Bob", null))
        val response = slot<PeopleList>()

        grpc.listPeople(ListPeopleRequest.getDefaultInstance(), observer)

        verifyOrder {
            observer.onNext(capture(response))
            observer.onCompleted()
        }
        assertEquals(listOf(1L, 2L), response.captured.peopleList.map { it.id })
        assertEquals(listOf("Anna", "Bob"), response.captured.peopleList.map { it.name })
        assertEquals(listOf("Actor", ""), response.captured.peopleList.map { it.bio })   // null bio becomes empty string
    }

    @Test
    fun `should answer an empty list when there are no people`() {
        every { personService.list() } returns emptyList()
        val response = slot<PeopleList>()

        grpc.listPeople(ListPeopleRequest.getDefaultInstance(), observer)

        verify { observer.onNext(capture(response)) }
        assertEquals(0, response.captured.peopleCount)
    }

    // E1-S2

    @Test
    fun `should create the person and answer it with its new id`() {
        every { personService.create("Anna", null) } returns Person(7, "Anna", null)
        val personObserver = mockk<StreamObserver<PersonProto>>(relaxed = true)
        val response = slot<PersonProto>()

        grpc.createPerson(CreatePersonRequest.newBuilder().setName("Anna").build(), personObserver)

        verifyOrder {
            personObserver.onNext(capture(response))
            personObserver.onCompleted()
        }
        assertEquals(7L, response.captured.id)
        assertEquals("", response.captured.bio)   // proto3: empty string carries "no bio"
    }

    @Test
    fun `should let a ValidationException escape so GrpcExceptionAdvice turns it into INVALID_ARGUMENT`() {
        every { personService.create("", null) } throws ValidationException("name must not be blank")
        val personObserver = mockk<StreamObserver<PersonProto>>(relaxed = true)

        assertThrows(ValidationException::class.java) {
            grpc.createPerson(CreatePersonRequest.getDefaultInstance(), personObserver)
        }
        verify(exactly = 0) { personObserver.onNext(any()) }
    }

    // E1-S3

    @Test
    fun `should answer the person by id`() {
        every { personService.get(3) } returns Person(3, "Anna", "Actor")
        val personObserver = mockk<StreamObserver<PersonProto>>(relaxed = true)
        val response = slot<PersonProto>()

        grpc.getPerson(GetPersonRequest.newBuilder().setId(3).build(), personObserver)

        verifyOrder {
            personObserver.onNext(capture(response))
            personObserver.onCompleted()
        }
        assertEquals("Actor", response.captured.bio)
    }

    @Test
    fun `should let a NotFoundException escape when getting a missing person`() {
        every { personService.get(99) } throws NotFoundException("Person", 99)
        val personObserver = mockk<StreamObserver<PersonProto>>(relaxed = true)

        assertThrows(NotFoundException::class.java) {
            grpc.getPerson(GetPersonRequest.newBuilder().setId(99).build(), personObserver)
        }
        verify(exactly = 0) { personObserver.onNext(any()) }
    }

    @Test
    fun `should update the person and answer the new values`() {
        every { personService.update(3, "Anna B", null) } returns Person(3, "Anna B", null)
        val personObserver = mockk<StreamObserver<PersonProto>>(relaxed = true)
        val response = slot<PersonProto>()

        grpc.updatePerson(UpdatePersonRequest.newBuilder().setId(3).setName("Anna B").build(), personObserver)

        verify { personObserver.onNext(capture(response)) }
        assertEquals("Anna B", response.captured.name)
    }

    @Test
    fun `should let a NotFoundException escape when updating a missing person`() {
        every { personService.update(99, "Anna", null) } throws NotFoundException("Person", 99)
        val personObserver = mockk<StreamObserver<PersonProto>>(relaxed = true)

        assertThrows(NotFoundException::class.java) {
            grpc.updatePerson(UpdatePersonRequest.newBuilder().setId(99).setName("Anna").build(), personObserver)
        }
    }

    // E1-S4

    @Test
    fun `should delete the person and complete with Empty`() {
        every { personService.delete(3) } returns Unit
        val emptyObserver = mockk<StreamObserver<Empty>>(relaxed = true)

        grpc.deletePerson(DeletePersonRequest.newBuilder().setId(3).build(), emptyObserver)

        verifyOrder {
            emptyObserver.onNext(Empty.getDefaultInstance())
            emptyObserver.onCompleted()
        }
    }

    @Test
    fun `should let a NotFoundException escape when deleting a missing person`() {
        every { personService.delete(99) } throws NotFoundException("Person", 99)
        val emptyObserver = mockk<StreamObserver<Empty>>(relaxed = true)

        assertThrows(NotFoundException::class.java) {
            grpc.deletePerson(DeletePersonRequest.newBuilder().setId(99).build(), emptyObserver)
        }
        verify(exactly = 0) { emptyObserver.onNext(any()) }
    }

    // E4-S1

    @Test
    fun `should answer the people behind the requested ids`() {
        every { personService.getMany(listOf(1L, 2L)) } returns listOf(Person(1, "Anna"), Person(2, "Bob"))
        val response = slot<PeopleList>()

        grpc.getPeople(GetPeopleRequest.newBuilder().addAllIds(listOf(1L, 2L)).build(), observer)

        verify { observer.onNext(capture(response)) }
        assertEquals(listOf(1L, 2L), response.captured.peopleList.map { it.id })
    }

    @Test
    fun `should answer the people matching a search`() {
        every { personService.search("nol") } returns listOf(Person(1, "Christopher Nolan"))
        val response = slot<PeopleList>()

        grpc.searchPeople(SearchPeopleRequest.newBuilder().setText("nol").build(), observer)

        verify { observer.onNext(capture(response)) }
        assertEquals(listOf("Christopher Nolan"), response.captured.peopleList.map { it.name })
    }
}
