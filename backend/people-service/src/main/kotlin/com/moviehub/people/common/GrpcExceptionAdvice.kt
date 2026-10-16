package com.moviehub.people.common

import com.moviehub.common.error.AppException
import com.moviehub.common.error.ErrorCode
import io.grpc.Status
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.advice.GrpcAdvice
import org.springframework.grpc.server.advice.GrpcExceptionHandler

/**
 * The only place a people-service exception becomes a gRPC status (ADR 0007).
 * movie-service's GrpcStatusMapper is the exact reverse of this table.
 */
@GrpcAdvice
class GrpcExceptionAdvice {

    private val log = LoggerFactory.getLogger(javaClass)

    @GrpcExceptionHandler(AppException::class)
    fun handleApp(ex: AppException): Status = ex.code.toStatus().withDescription(ex.message)

    @GrpcExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): Status {
        log.error("Unhandled exception in gRPC service", ex)
        return Status.INTERNAL.withDescription("Something went wrong")
    }

    private fun ErrorCode.toStatus(): Status = when (this) {
        ErrorCode.NOT_FOUND -> Status.NOT_FOUND
        ErrorCode.VALIDATION -> Status.INVALID_ARGUMENT
        ErrorCode.CONFLICT -> Status.ALREADY_EXISTS
        ErrorCode.UNAVAILABLE -> Status.UNAVAILABLE
        ErrorCode.INTERNAL -> Status.INTERNAL
    }
}
