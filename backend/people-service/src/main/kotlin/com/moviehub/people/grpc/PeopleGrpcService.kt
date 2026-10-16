package com.moviehub.people.grpc

import com.google.protobuf.Empty
import com.moviehub.contracts.people.v1.CreatePersonRequest
import com.moviehub.contracts.people.v1.DeletePersonRequest
import com.moviehub.contracts.people.v1.GetPeopleRequest
import com.moviehub.contracts.people.v1.GetPersonRequest
import com.moviehub.contracts.people.v1.ListPeopleRequest
import com.moviehub.contracts.people.v1.PeopleList
import com.moviehub.contracts.people.v1.PeopleServiceGrpc
import com.moviehub.contracts.people.v1.SearchPeopleRequest
import com.moviehub.contracts.people.v1.UpdatePersonRequest
import com.moviehub.contracts.people.v1.Person as PersonProto
import com.moviehub.people.domain.Person
import com.moviehub.people.service.PersonService
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

/**
 * gRPC entry point of people-service: the only class here that sees gRPC types (coding-standards.md).
 * Each RPC maps the request to one service call and the result to proto. RPCs not yet overridden answer UNIMPLEMENTED.
 * Exceptions are turned into gRPC statuses by GrpcExceptionAdvice (ADR 0007).
 */
@GrpcService
class PeopleGrpcService(private val personService: PersonService) : PeopleServiceGrpc.PeopleServiceImplBase() {

    override fun listPeople(request: ListPeopleRequest, responseObserver: StreamObserver<PeopleList>) {
        val people = personService.list().map { it.toProto() }
        responseObserver.onNext(PeopleList.newBuilder().addAllPeople(people).build())
        responseObserver.onCompleted()
    }

    override fun createPerson(request: CreatePersonRequest, responseObserver: StreamObserver<PersonProto>) {
        val person = personService.create(request.name, request.bio.ifEmpty { null })
        responseObserver.onNext(person.toProto())
        responseObserver.onCompleted()
    }

    override fun getPeople(request: GetPeopleRequest, responseObserver: StreamObserver<PeopleList>) {
        val people = personService.getMany(request.idsList).map { it.toProto() }
        responseObserver.onNext(PeopleList.newBuilder().addAllPeople(people).build())
        responseObserver.onCompleted()
    }

    override fun searchPeople(request: SearchPeopleRequest, responseObserver: StreamObserver<PeopleList>) {
        val people = personService.search(request.text).map { it.toProto() }
        responseObserver.onNext(PeopleList.newBuilder().addAllPeople(people).build())
        responseObserver.onCompleted()
    }

    override fun getPerson(request: GetPersonRequest, responseObserver: StreamObserver<PersonProto>) {
        responseObserver.onNext(personService.get(request.id).toProto())
        responseObserver.onCompleted()
    }

    override fun updatePerson(request: UpdatePersonRequest, responseObserver: StreamObserver<PersonProto>) {
        val person = personService.update(request.id, request.name, request.bio.ifEmpty { null })
        responseObserver.onNext(person.toProto())
        responseObserver.onCompleted()
    }

    override fun deletePerson(request: DeletePersonRequest, responseObserver: StreamObserver<Empty>) {
        personService.delete(request.id)
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    private fun Person.toProto(): PersonProto = PersonProto.newBuilder()
        .setId(requireNotNull(id) { "only saved people cross the wire" })
        .setName(name)
        .setBio(bio ?: "")          // proto3 strings cannot be null; empty means "not set"
        .build()
}
