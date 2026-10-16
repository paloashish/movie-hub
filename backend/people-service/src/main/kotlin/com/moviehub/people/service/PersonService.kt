package com.moviehub.people.service

import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import com.moviehub.people.domain.Person
import com.moviehub.people.repository.PersonRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PersonService(private val repository: PersonRepository) {

    /** E1-S1: everyone, A to Z regardless of case, so "anna" does not sort after "Zoe". */
    fun list(): List<Person> = repository.findAll(Sort.by(Sort.Order.asc("name").ignoreCase()))

    /**
     * E1-S2: create a person. The GraphQL edge already validates the input, but gRPC is a public door
     * into this service, so the rules are checked again here (application-architecture 4.2: server is the truth).
     */
    @Transactional
    fun create(name: String, bio: String?): Person {
        val (cleanName, cleanBio) = validated(name, bio)
        return repository.save(Person(name = cleanName, bio = cleanBio))
    }

    /** E1-S3: one person, or NotFoundException (gRPC NOT_FOUND, GraphQL code NOT_FOUND). */
    fun get(id: Long): Person = repository.findById(id).orElseThrow { NotFoundException("Person", id) }

    /** E1-S3: same rules as create; a person deleted meanwhile answers NOT_FOUND. */
    @Transactional
    fun update(id: Long, name: String, bio: String?): Person {
        val (cleanName, cleanBio) = validated(name, bio)
        val person = get(id)
        person.name = cleanName
        person.bio = cleanBio
        person.updatedAt = LocalDateTime.now()
        return repository.save(person)
    }

    /** E1-S4: deleting a missing person is NOT_FOUND, so a double click or a stale page gets a clear answer. */
    @Transactional
    fun delete(id: Long) {
        repository.delete(get(id))
    }

    /** E4-S1: the people behind a movie's credits, by id; missing ids are simply absent (a credit may be stale). */
    fun getMany(ids: Collection<Long>): List<Person> = if (ids.isEmpty()) emptyList() else repository.findAllById(ids)

    /** E4-S1 picker, E5-S1 search: partial, case-insensitive match on the name; blank text matches nothing. */
    fun search(text: String): List<Person> {
        val clean = text.trim()
        return if (clean.isEmpty()) emptyList() else repository.findByNameContainingIgnoreCaseOrderByNameAsc(clean)
    }

    private fun validated(name: String, bio: String?): Pair<String, String?> {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) throw ValidationException("name must not be blank")
        if (cleanName.length > NAME_MAX) throw ValidationException("name must be at most $NAME_MAX characters")
        val cleanBio = bio?.trim()?.ifEmpty { null }
        if (cleanBio != null && cleanBio.length > BIO_MAX) throw ValidationException("bio must be at most $BIO_MAX characters")
        return cleanName to cleanBio
    }

    private companion object {
        // Same limits as the person table (0001-E1-S1-create-person.xml)
        const val NAME_MAX = 200
        const val BIO_MAX = 2000
    }
}
