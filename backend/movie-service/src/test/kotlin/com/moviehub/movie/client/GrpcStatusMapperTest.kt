package com.moviehub.movie.client

import com.moviehub.common.error.ConflictException
import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.common.error.ErrorCode
import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class GrpcStatusMapperTest {

    @Test
    fun `should map NOT_FOUND status to NotFoundException keeping the id`() {
        val ex = GrpcStatusMapper.toAppException(Status.NOT_FOUND.withDescription("Person 7 not found").asRuntimeException())

        assertInstanceOf(NotFoundException::class.java, ex)
        assertEquals("Person 7 not found", ex.message)
    }

    @Test
    fun `should map INVALID_ARGUMENT status to ValidationException with the description`() {
        val ex = GrpcStatusMapper.toAppException(Status.INVALID_ARGUMENT.withDescription("name must not be blank").asRuntimeException())

        assertInstanceOf(ValidationException::class.java, ex)
        assertEquals("name must not be blank", ex.message)
    }

    @Test
    fun `should map ALREADY_EXISTS status to ConflictException`() {
        val ex = GrpcStatusMapper.toAppException(Status.ALREADY_EXISTS.withDescription("duplicate").asRuntimeException())

        assertInstanceOf(ConflictException::class.java, ex)
    }

    @Test
    fun `should map UNAVAILABLE status to DependencyUnavailableException naming people-service`() {
        val ex = GrpcStatusMapper.toAppException(Status.UNAVAILABLE.asRuntimeException())

        assertInstanceOf(DependencyUnavailableException::class.java, ex)
        assertEquals("people-service is unavailable", ex.message)
    }

    @Test
    fun `should treat any other status as UNAVAILABLE so the UI offers retry`() {
        val ex = GrpcStatusMapper.toAppException(Status.INTERNAL.withDescription("boom").asRuntimeException())

        assertEquals(ErrorCode.UNAVAILABLE, ex.code)
    }

    @Test
    fun `should not crash when the status has no description`() {
        val ex = GrpcStatusMapper.toAppException(Status.INVALID_ARGUMENT.asRuntimeException())

        assertEquals("INVALID_ARGUMENT", ex.message)
    }
}
