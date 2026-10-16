package com.moviehub.common.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppExceptionTest {

    @Test
    fun `should carry NOT_FOUND code and name the missing thing when not found`() {
        val ex = NotFoundException("Movie", 42)

        assertEquals(ErrorCode.NOT_FOUND, ex.code)
        assertEquals("Movie 42 not found", ex.message)
    }

    @Test
    fun `should carry VALIDATION code when validation fails`() {
        val ex = ValidationException("title must not be blank")

        assertEquals(ErrorCode.VALIDATION, ex.code)
        assertEquals("title must not be blank", ex.message)
    }

    @Test
    fun `should carry CONFLICT code when a duplicate is rejected`() {
        val ex = ConflictException("already credited")

        assertEquals(ErrorCode.CONFLICT, ex.code)
    }

    @Test
    fun `should carry UNAVAILABLE code and name the dependency when a dependency is down`() {
        val ex = DependencyUnavailableException("people-service")

        assertEquals(ErrorCode.UNAVAILABLE, ex.code)
        assertEquals("people-service is unavailable", ex.message)
    }
}
