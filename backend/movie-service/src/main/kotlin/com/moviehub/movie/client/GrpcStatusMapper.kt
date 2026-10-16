package com.moviehub.movie.client

import com.moviehub.common.error.AppException
import com.moviehub.common.error.ConflictException
import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import io.grpc.Status
import io.grpc.StatusRuntimeException

/**
 * Reverse of people-service's GrpcExceptionAdvice: a gRPC status coming back over the wire
 * becomes the same AppException the rest of movie-service already understands (ADR 0007).
 */
object GrpcStatusMapper {

    fun toAppException(ex: StatusRuntimeException): AppException {
        val description = ex.status.description ?: ex.status.code.name
        return when (ex.status.code) {
            Status.Code.NOT_FOUND -> NotFoundException(description)   // people-service already wrote "Person 7 not found"
            Status.Code.INVALID_ARGUMENT -> ValidationException(description)
            Status.Code.ALREADY_EXISTS -> ConflictException(description)
            Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED -> DependencyUnavailableException("people-service")
            else -> DependencyUnavailableException("people-service")
        }
    }
}
