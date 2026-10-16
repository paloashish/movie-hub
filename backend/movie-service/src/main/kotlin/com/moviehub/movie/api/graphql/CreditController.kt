package com.moviehub.movie.api.graphql

import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.Person
import com.moviehub.movie.domain.Role
import com.moviehub.movie.service.CreditService
import com.moviehub.movie.service.CreditWithPerson
import com.moviehub.movie.service.PersonCredit
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

/** GraphQL edge for credits: the Movie.credits field and the credit mutations. No logic here. */
@Controller
class CreditController(private val creditService: CreditService) {

    /** E4-S1. Resolved only when a query asks for `credits`, so the grid never pays for the gRPC call. */
    @SchemaMapping(typeName = "Movie", field = "credits")
    fun credits(movie: Movie): List<CreditWithPerson> = creditService.listForMovie(requireNotNull(movie.id))

    /** E4-S3. Resolved only when a query asks for a person's `credits`. */
    @SchemaMapping(typeName = "Person", field = "credits")
    fun personCredits(person: Person): List<PersonCredit> = creditService.listForPerson(person.id)

    /** E4-S1 */
    @MutationMapping
    fun addCredit(@Argument movieId: Long, @Argument personId: Long, @Argument role: Role): CreditWithPerson =
        creditService.add(movieId, personId, role)

    /** E4-S2. Returns true because GraphQL needs a value and the credit is gone. */
    @MutationMapping
    fun removeCredit(@Argument id: Long): Boolean {
        creditService.remove(id)
        return true
    }
}
