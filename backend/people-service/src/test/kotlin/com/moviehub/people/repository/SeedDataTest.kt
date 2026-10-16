package com.moviehub.people.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

/** E6-S2: with the seed context on, an empty database gets 20 people with the ids the movie seed's credits expect. */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.liquibase.contexts=schema,seed"])
class SeedDataTest(@Autowired private val repository: PersonRepository) {

    @Test
    fun `should seed exactly 20 people with ids 100001 to 100020`() {
        val all = repository.findAll()

        assertEquals(20, all.size)
        assertEquals((100001L..100020L).toList(), all.map { requireNotNull(it.id) }.sorted())
        all.forEach { assertTrue(it.name.isNotBlank()) }
    }
}
