package com.moviehub.movie.service

import com.moviehub.common.error.ConflictException
import com.moviehub.common.error.NotFoundException
import com.moviehub.movie.client.PeopleClient
import com.moviehub.movie.domain.Credit
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.domain.Person
import com.moviehub.movie.domain.Role
import com.moviehub.movie.repository.CreditRepository
import com.moviehub.movie.repository.MovieRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** A credit with its person resolved from people-service. What the movie page shows. */
data class CreditWithPerson(val id: Long, val role: Role, val person: Person)

/** A credit seen from the person's side: the movie it is on. What the person page shows (E4-S3). */
data class PersonCredit(val id: Long, val role: Role, val movie: Movie)

/**
 * Credits link the two services: the row lives here, the person lives in people-service (ADR 0001).
 * Every read resolves people in one gRPC call; every write checks the person exists first.
 */
@Service
class CreditService(
    private val credits: CreditRepository,
    private val movies: MovieRepository,
    private val movieService: MovieService,
    private val peopleClient: PeopleClient,
) {

    /** E4-S1: the movie's credits with names. A credit whose person vanished is skipped rather than shown blank. */
    fun listForMovie(movieId: Long): List<CreditWithPerson> {
        val rows = credits.findByMovieIdOrderByIdAsc(movieId)
        val people = peopleClient.getPeople(rows.map { it.personId }.distinct()).associateBy { it.id }
        return rows.mapNotNull { row ->
            people[row.personId]?.let { CreditWithPerson(requireNotNull(row.id), row.role, it) }
        }
    }

    /** E4-S1: both sides must exist, and the same person cannot hold the same role twice on one movie. */
    @Transactional
    fun add(movieId: Long, personId: Long, role: Role): CreditWithPerson {
        movieService.get(movieId)
        val person = peopleClient.getPerson(personId)   // NOT_FOUND if the person is gone
        if (credits.existsByMovieIdAndPersonIdAndRole(movieId, personId, role)) {
            throw ConflictException("${person.name} is already credited as ${role.name.lowercase()}")
        }
        val saved = try {
            credits.saveAndFlush(Credit(movieId = movieId, personId = personId, role = role))
        } catch (e: DataIntegrityViolationException) {
            // Two requests passed the check above at the same time; the unique constraint is the final judge
            throw ConflictException("${person.name} is already credited as ${role.name.lowercase()}")
        }
        return CreditWithPerson(requireNotNull(saved.id), saved.role, person)
    }

    /** E4-S2: remove one credit. The person stays in the directory; only the link goes. */
    @Transactional
    fun remove(creditId: Long) {
        val credit = credits.findById(creditId).orElseThrow { NotFoundException("Credit", creditId) }
        credits.delete(credit)
    }

    /** E4-S3: a person's movies with their role, newest first; same-year movies A to Z so the order is stable. Movies live here: no gRPC hop. */
    fun listForPerson(personId: Long): List<PersonCredit> {
        val rows = credits.findByPersonId(personId)
        val byId = movies.findAllById(rows.map { it.movieId }.distinct()).associateBy { it.id }
        // The FK with cascade guarantees every credit's movie exists; the null branch only exists because Map.get is nullable
        return rows.mapNotNull { row -> byId[row.movieId]?.let { PersonCredit(requireNotNull(row.id), row.role, it) } }
            .sortedWith(compareByDescending<PersonCredit> { it.movie.releaseYear }.thenBy { it.movie.title.lowercase() })
    }

    /**
     * E4-S3 (system-design 3.3): deleting a person removes their credits here first, then the person over gRPC,
     * in one transaction. If the gRPC call fails, the credit delete rolls back and nothing has changed.
     */
    @Transactional
    fun removePerson(personId: Long) {
        credits.deleteByPersonId(personId)
        peopleClient.deletePerson(personId)
    }
}
