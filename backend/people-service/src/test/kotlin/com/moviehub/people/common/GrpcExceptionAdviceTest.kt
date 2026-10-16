package com.moviehub.people.common

import com.moviehub.common.error.ConflictException
import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GrpcExceptionAdviceTest {

    private val advice = GrpcExceptionAdvice()

    @Test
    fun `should map NotFoundException to NOT_FOUND with the message as description`() {
        val status = advice.handleApp(NotFoundException("Person", 7))

        assertEquals(Status.Code.NOT_FOUND, status.code)
        assertEquals("Person 7 not found", status.description)
    }

    @Test
    fun `should map ValidationException to INVALID_ARGUMENT`() {
        assertEquals(Status.Code.INVALID_ARGUMENT, advice.handleApp(ValidationException("name must not be blank")).code)
    }

    @Test
    fun `should map ConflictException to ALREADY_EXISTS`() {
        assertEquals(Status.Code.ALREADY_EXISTS, advice.handleApp(ConflictException("duplicate")).code)
    }

    @Test
    fun `should map DependencyUnavailableException to UNAVAILABLE`() {
        assertEquals(Status.Code.UNAVAILABLE, advice.handleApp(DependencyUnavailableException("database")).code)
    }

    @Test
    fun `should hide the cause and return INTERNAL when an unexpected exception is thrown`() {
        val status = advice.handleUnexpected(IllegalStateException("disk full"))

        assertEquals(Status.Code.INTERNAL, status.code)
        assertEquals("Something went wrong", status.description)
    }
}
