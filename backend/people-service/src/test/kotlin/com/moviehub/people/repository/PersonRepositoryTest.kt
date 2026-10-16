package com.moviehub.people.repository

import com.moviehub.people.domain.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles

/** Runs the Liquibase changeset against in-memory H2 and proves the entity matches the table. */
@DataJpaTest
@ActiveProfiles("test")
class PersonRepositoryTest(@Autowired private val repository: PersonRepository) {

    @Test
    fun `should save and read back a person with a generated id`() {
        val saved = repository.save(Person(name = "Christopher Nolan", bio = "Director"))

        val found = repository.findById(requireNotNull(saved.id)).orElseThrow()

        assertEquals("Christopher Nolan", found.name)
        assertEquals("Director", found.bio)
    }

    @Test
    fun `should sort by name ignoring case when asked`() {
        repository.saveAll(listOf(Person(name = "zoe"), Person(name = "Anna"), Person(name = "mark")))

        val names = repository.findAll(Sort.by(Sort.Order.asc("name").ignoreCase())).map { it.name }

        assertEquals(listOf("Anna", "mark", "zoe"), names)
    }

    @Test
    fun `should allow a person without a bio`() {
        val saved = repository.save(Person(name = "Unknown Extra"))

        assertEquals(null, repository.findById(requireNotNull(saved.id)).orElseThrow().bio)
    }

    @Test
    fun `should find people by a partial name regardless of case`() {
        repository.saveAll(listOf(Person(name = "Christopher Nolan"), Person(name = "Nolan Ryan"), Person(name = "Anna")))

        val names = repository.findByNameContainingIgnoreCaseOrderByNameAsc("NOL").map { it.name }

        assertEquals(listOf("Christopher Nolan", "Nolan Ryan"), names)
    }

    @Test
    fun `should treat the SQL wildcards percent and underscore as plain characters when searching`() {
        repository.saveAll(listOf(Person(name = "Mr. 100%"), Person(name = "Anna")))

        assertEquals(listOf("Mr. 100%"), repository.findByNameContainingIgnoreCaseOrderByNameAsc("%").map { it.name })
        assertEquals(emptyList<String>(), repository.findByNameContainingIgnoreCaseOrderByNameAsc("_").map { it.name })
    }
}
