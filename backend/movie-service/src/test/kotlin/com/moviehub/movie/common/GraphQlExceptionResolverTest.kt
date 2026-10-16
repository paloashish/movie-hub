package com.moviehub.movie.common

import com.moviehub.common.error.ConflictException
import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.stereotype.Controller

@GraphQlTest(controllers = [GraphQlExceptionResolverTest.ThrowingController::class])
@Import(GraphQlExceptionResolver::class, GraphQlExceptionResolverTest.ThrowingController::class)
class GraphQlExceptionResolverTest(@Autowired private val tester: GraphQlTester) {

    @Controller
    class ThrowingController {
        @QueryMapping fun throwNotFound(): String = throw NotFoundException("Movie", 42)
        @QueryMapping fun throwValidation(): String = throw ValidationException("title must not be blank")
        @QueryMapping fun throwConflict(): String = throw ConflictException("already credited")
        @QueryMapping fun throwUnavailable(): String = throw DependencyUnavailableException("people-service")
        @QueryMapping fun throwUnexpected(): String = throw IllegalStateException("db connection lost")
    }

    private fun codeAndMessageOf(field: String): Pair<String?, String> {
        var result: Pair<String?, String> = null to ""
        tester.document("{ $field }").execute().errors().satisfy { errors ->
            assertEquals(1, errors.size)
            result = errors[0].extensions["code"] as String? to errors[0].message!!
        }
        return result
    }

    @Test
    fun `should map NotFoundException to code NOT_FOUND with its message`() {
        assertEquals("NOT_FOUND" to "Movie 42 not found", codeAndMessageOf("throwNotFound"))
    }

    @Test
    fun `should map ValidationException to code VALIDATION`() {
        assertEquals("VALIDATION" to "title must not be blank", codeAndMessageOf("throwValidation"))
    }

    @Test
    fun `should map ConflictException to code CONFLICT`() {
        assertEquals("CONFLICT" to "already credited", codeAndMessageOf("throwConflict"))
    }

    @Test
    fun `should map DependencyUnavailableException to code UNAVAILABLE`() {
        assertEquals("UNAVAILABLE" to "people-service is unavailable", codeAndMessageOf("throwUnavailable"))
    }

    @Test
    fun `should hide the cause and return INTERNAL when an unexpected exception is thrown`() {
        val (code, message) = codeAndMessageOf("throwUnexpected")

        assertEquals("INTERNAL", code)
        assertEquals("Something went wrong", message)   // never leaks "db connection lost"
    }
}
