package com.moviehub.common.error

/**
 * The one error vocabulary shared by both services (ADR 0007).
 * Edge mappers (GraphQL, REST, gRPC) translate these codes; nothing else decides HTTP or gRPC statuses.
 * INTERNAL is set only by the edge mappers for unexpected exceptions, never thrown by services.
 */
enum class ErrorCode { NOT_FOUND, VALIDATION, CONFLICT, UNAVAILABLE, INTERNAL }

/** Base of every exception a service is allowed to throw towards an edge. Sealed so mappers can be exhaustive. */
sealed class AppException(val code: ErrorCode, message: String) : RuntimeException(message)

class NotFoundException(message: String) : AppException(ErrorCode.NOT_FOUND, message) {
    /** The usual form: NotFoundException("Movie", 42) -> "Movie 42 not found". */
    constructor(what: String, id: Any) : this("$what $id not found")
}

class ValidationException(message: String) : AppException(ErrorCode.VALIDATION, message)

class ConflictException(message: String) : AppException(ErrorCode.CONFLICT, message)

class DependencyUnavailableException(dependency: String) :
    AppException(ErrorCode.UNAVAILABLE, "$dependency is unavailable")
