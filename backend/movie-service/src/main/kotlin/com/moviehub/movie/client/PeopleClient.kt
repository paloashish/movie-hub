package com.moviehub.movie.client

import com.moviehub.contracts.people.v1.CreatePersonRequest
import com.moviehub.contracts.people.v1.DeletePersonRequest
import com.moviehub.contracts.people.v1.GetPeopleRequest
import com.moviehub.contracts.people.v1.GetPersonRequest
import com.moviehub.contracts.people.v1.ListPeopleRequest
import com.moviehub.contracts.people.v1.PeopleServiceGrpc
import com.moviehub.contracts.people.v1.SearchPeopleRequest
import com.moviehub.contracts.people.v1.UpdatePersonRequest
import com.moviehub.contracts.people.v1.Person as PersonProto
import com.moviehub.movie.domain.Person
import io.grpc.StatusRuntimeException
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * The only class in movie-service that sees gRPC types (coding-standards.md).
 * The blocking stub is created by Spring Boot from @ImportGrpcClients on the application class
 * and the `spring.grpc.client.channel.people.target` property. Methods are added one story at a time.
 */
@Component
class PeopleClient(private val stub: PeopleServiceGrpc.PeopleServiceBlockingStub) {

    /** E1-S1: everyone, already sorted by people-service. */
    fun listPeople(): List<Person> =
        call { listPeople(ListPeopleRequest.getDefaultInstance()) }.peopleList.map { it.toDomain() }

    /** E1-S2: create a person; people-service answers with the saved person including its id. */
    fun createPerson(name: String, bio: String?): Person {
        val request = CreatePersonRequest.newBuilder().setName(name).setBio(bio ?: "").build()
        return call { createPerson(request) }.toDomain()
    }

    /** E4-S1: the people behind a movie's credits in one call; ids that no longer exist are absent. */
    fun getPeople(ids: Collection<Long>): List<Person> =
        if (ids.isEmpty()) emptyList()
        else call { getPeople(GetPeopleRequest.newBuilder().addAllIds(ids).build()) }.peopleList.map { it.toDomain() }

    /** E4-S1 picker: people whose name contains the text. */
    fun searchPeople(text: String): List<Person> =
        call { searchPeople(SearchPeopleRequest.newBuilder().setText(text).build()) }.peopleList.map { it.toDomain() }

    /** E1-S3: one person; a missing id surfaces as NotFoundException via the status mapper. */
    fun getPerson(id: Long): Person =
        call { getPerson(GetPersonRequest.newBuilder().setId(id).build()) }.toDomain()

    /** E1-S3 */
    fun updatePerson(id: Long, name: String, bio: String?): Person {
        val request = UpdatePersonRequest.newBuilder().setId(id).setName(name).setBio(bio ?: "").build()
        return call { updatePerson(request) }.toDomain()
    }

    /** E1-S4 */
    fun deletePerson(id: Long) {
        call { deletePerson(DeletePersonRequest.newBuilder().setId(id).build()) }
    }

    /**
     * Runs one gRPC call with a deadline and converts any gRPC failure into an AppException.
     * `block` is a lambda that runs with the stub as `this`, so callers write `call { listPeople(req) }`.
     * The deadline keeps a hung people-service from leaving the page in its loading state forever;
     * DEADLINE_EXCEEDED maps to UNAVAILABLE like any other outage.
     */
    private fun <T> call(block: PeopleServiceGrpc.PeopleServiceBlockingStub.() -> T): T =
        try {
            stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS).block()
        } catch (ex: StatusRuntimeException) {
            throw GrpcStatusMapper.toAppException(ex)
        }

    private fun PersonProto.toDomain() = Person(id = id, name = name, bio = bio.ifEmpty { null })

    private companion object {
        const val DEADLINE_SECONDS = 5L
    }
}
