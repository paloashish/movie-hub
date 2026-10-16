package com.moviehub.movie.api.graphql

import com.moviehub.movie.client.PeopleClient
import com.moviehub.movie.domain.Person
import com.moviehub.movie.service.CreditService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

/** GraphQL edge for people. Maps queries to PeopleClient calls; no logic here (coding-standards.md). */
@Controller
class PersonController(private val peopleClient: PeopleClient, private val creditService: CreditService) {

    @QueryMapping
    fun people(): List<Person> = peopleClient.listPeople()

    /** E1-S3. The ID scalar arrives as a string; Spring converts it to Long before the body runs. */
    @QueryMapping
    fun person(@Argument id: Long): Person = peopleClient.getPerson(id)

    /** E4-S1: the credit picker's name search. */
    @QueryMapping
    fun searchPeople(@Argument text: String): List<Person> = peopleClient.searchPeople(text)

    /** E1-S2. `@Valid` makes Spring run the PersonInput constraints before this body executes. */
    @MutationMapping
    fun createPerson(@Argument @Valid input: PersonInput): Person = peopleClient.createPerson(input.name, input.bio)

    /** E1-S3 */
    @MutationMapping
    fun updatePerson(@Argument id: Long, @Argument @Valid input: PersonInput): Person =
        peopleClient.updatePerson(id, input.name, input.bio)

    /** E1-S4 + E4-S3. Credits go first, then the person, in one transaction (system-design 3.3). */
    @MutationMapping
    fun deletePerson(@Argument id: Long): Boolean {
        creditService.removePerson(id)
        return true
    }
}
