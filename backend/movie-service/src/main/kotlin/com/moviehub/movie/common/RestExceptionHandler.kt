package com.moviehub.movie.common

import com.moviehub.common.error.AppException
import com.moviehub.common.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Error mapping for the one REST endpoint (poster upload, ADR 0003) and anything else over plain HTTP.
 * Produces RFC 9457 problem details with the same `code` vocabulary as GraphQL (ADR 0007).
 */
@RestControllerAdvice
class RestExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(AppException::class)
    fun handleApp(ex: AppException): ProblemDetail = problem(ex.code, ex.message ?: ex.code.name)

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleTooLarge(ex: MaxUploadSizeExceededException): ProblemDetail =
        problem(ErrorCode.VALIDATION, "file must be at most ${ex.maxUploadSize / (1024 * 1024)} MB")

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleUnknownPath(ex: NoResourceFoundException): ProblemDetail =
        problem(ErrorCode.NOT_FOUND, "No such endpoint: ${ex.resourcePath}")

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ProblemDetail {
        // Spring MVC's own exceptions (405, 415, malformed JSON, ...) already carry a status and a ProblemDetail.
        // Keep that status and only add our code; they are client errors, not server failures.
        if (ex is ErrorResponse) {
            return ex.body.apply { setProperty("code", codeForStatus(ex.statusCode.value()).name) }
        }
        log.error("Unhandled exception in REST endpoint", ex)
        return problem(ErrorCode.INTERNAL, "Something went wrong")
    }

    private fun codeForStatus(status: Int): ErrorCode = when {
        status == 404 -> ErrorCode.NOT_FOUND
        status in 400..499 -> ErrorCode.VALIDATION
        else -> ErrorCode.INTERNAL
    }

    private fun problem(code: ErrorCode, message: String): ProblemDetail =
        ProblemDetail.forStatusAndDetail(code.toStatus(), message).apply {
            title = code.name
            setProperty("code", code.name)
        }

    private fun ErrorCode.toStatus(): HttpStatus = when (this) {
        ErrorCode.NOT_FOUND -> HttpStatus.NOT_FOUND
        ErrorCode.VALIDATION -> HttpStatus.BAD_REQUEST
        ErrorCode.CONFLICT -> HttpStatus.CONFLICT
        ErrorCode.UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE
        ErrorCode.INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR
    }
}
