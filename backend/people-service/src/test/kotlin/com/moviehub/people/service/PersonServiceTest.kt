package com.moviehub.people.service

import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import com.moviehub.people.domain.Person
import com.moviehub.people.repository.PersonRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import java.util.Optional

class PersonServiceTest {

    private val repository = mockk<PersonRepository>()
    private val service = PersonService(repository)

    @Test
    fun `should return everyone sorted by name ignoring case`() {
        val sort = slot<Sort>()
        every { repository.findAll(capture(sort)) } returns listOf(Person(1, "anna"), Person(2, "Zoe"))

        val result = service.list()

        assertEquals(listOf("anna", "Zoe"), result.map { it.name })
        val order = requireNotNull(sort.captured.getOrderFor("name")) { "service must sort by name" }
        assertTrue(order.isAscending)
        assertTrue(order.isIgnoreCase)
    }

    @Test
    fun `should return an empty list when there are no people`() {
        every { repository.findAll(any<Sort>()) } returns emptyList()

        assertEquals(emptyList<Person>(), service.list())
    }

    // E1-S2

    @Test
    fun `should save a person with trimmed name and bio when creating`() {
        val saved = slot<Person>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        val person = service.create("  Anna Example ", " Actor ")

        assertEquals("Anna Example", person.name)
        assertEquals("Actor", person.bio)
    }

    @Test
    fun `should store a blank bio as null when creating`() {
        every { repository.save(any()) } answers { firstArg() }

        assertNull(service.create("Anna", "   ").bio)
    }

    @Test
    fun `should store a missing bio as null when creating`() {
        every { repository.save(any()) } answers { firstArg() }

        assertNull(service.create("Anna", null).bio)
    }

    @Test
    fun `should throw ValidationException and save nothing when the name is blank`() {
        val ex = assertThrows(ValidationException::class.java) { service.create("   ", null) }

        assertEquals("name must not be blank", ex.message)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should throw ValidationException when the name is longer than 200 characters`() {
        assertThrows(ValidationException::class.java) { service.create("a".repeat(201), null) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should throw ValidationException when the bio is longer than 2000 characters`() {
        assertThrows(ValidationException::class.java) { service.create("Anna", "b".repeat(2001)) }
        verify(exactly = 0) { repository.save(any()) }
    }

    // E1-S3

    @Test
    fun `should return the person when it exists`() {
        every { repository.findById(1) } returns Optional.of(Person(1, "Anna"))

        assertEquals("Anna", service.get(1).name)
    }

    @Test
    fun `should throw NotFoundException when getting a missing person`() {
        every { repository.findById(99) } returns Optional.empty()

        val ex = assertThrows(NotFoundException::class.java) { service.get(99) }

        assertEquals("Person 99 not found", ex.message)
    }

    @Test
    fun `should update name and bio, trimmed, and bump updatedAt`() {
        val existing = Person(1, "Old", "Old bio")
        val before = existing.updatedAt
        every { repository.findById(1) } returns Optional.of(existing)
        every { repository.save(any()) } answers { firstArg() }

        val updated = service.update(1, " New ", " New bio ")

        assertEquals("New", updated.name)
        assertEquals("New bio", updated.bio)
        assertFalse(updated.updatedAt.isBefore(before))
    }

    @Test
    fun `should throw NotFoundException and save nothing when updating a missing person`() {
        every { repository.findById(99) } returns Optional.empty()

        assertThrows(NotFoundException::class.java) { service.update(99, "Anna", null) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should throw ValidationException before looking the person up when the new name is blank`() {
        assertThrows(ValidationException::class.java) { service.update(1, "  ", null) }
        verify(exactly = 0) { repository.findById(any()) }
    }

    // E1-S4

    @Test
    fun `should delete the person when it exists`() {
        val existing = Person(1, "Anna")
        every { repository.findById(1) } returns Optional.of(existing)
        every { repository.delete(existing) } returns Unit

        service.delete(1)

        verify(exactly = 1) { repository.delete(existing) }
    }

    @Test
    fun `should throw NotFoundException and delete nothing when the person is missing`() {
        every { repository.findById(99) } returns Optional.empty()

        assertThrows(NotFoundException::class.java) { service.delete(99) }
        verify(exactly = 0) { repository.delete(any()) }
    }

    // E4-S1

    @Test
    fun `should return the people behind the given ids`() {
        every { repository.findAllById(listOf(1L, 2L)) } returns listOf(Person(1, "Anna"), Person(2, "Bob"))

        assertEquals(listOf("Anna", "Bob"), service.getMany(listOf(1L, 2L)).map { it.name })
    }

    @Test
    fun `should not hit the database for an empty id list`() {
        assertEquals(emptyList<Person>(), service.getMany(emptyList()))
        verify(exactly = 0) { repository.findAllById(any()) }
    }

    @Test
    fun `should search by partial name ignoring case`() {
        every { repository.findByNameContainingIgnoreCaseOrderByNameAsc("nol") } returns listOf(Person(1, "Christopher Nolan"))

        assertEquals(listOf("Christopher Nolan"), service.search("  nol ").map { it.name })
    }

    @Test
    fun `should return nothing for a blank search without touching the database`() {
        assertEquals(emptyList<Person>(), service.search("   "))
        verify(exactly = 0) { repository.findByNameContainingIgnoreCaseOrderByNameAsc(any()) }
    }
}
