package com.moviehub.movie.common

import com.moviehub.common.error.AppException
import com.moviehub.common.error.ErrorCode
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component
import org.springframework.validation.BindException

/**
 * The only place a service exception becomes a GraphQL error (ADR 0007).
 * Every error carries `extensions.code`, which is what the frontend switches on.
 */
@Component
class GraphQlExceptionResolver : DataFetcherExceptionResolverAdapter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError = when (ex) {
        is AppException -> error(env, ex.code, ex.message ?: ex.code.name)
        is ConstraintViolationException -> error(env, ErrorCode.VALIDATION, violationsMessage(ex))
        is BindException -> error(env, ErrorCode.VALIDATION, bindMessage(ex))
        else -> {
            log.error("Unhandled exception in GraphQL field {}", env.field.name, ex)
            error(env, ErrorCode.INTERNAL, "Something went wrong")
        }
    }

    private fun error(env: DataFetchingEnvironment, code: ErrorCode, message: String): GraphQLError =
        GraphqlErrorBuilder.newError(env)
            .errorType(code.toErrorType())
            .message(message)
            .extensions(mapOf("code" to code.name))
            .build()

    private fun violationsMessage(ex: ConstraintViolationException): String =
        ex.constraintViolations.joinToString("; ") { "${it.propertyPath.last()}: ${it.message}" }

    private fun bindMessage(ex: BindException): String =
        ex.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }

    /** Spring's classification, useful in GraphiQL; `extensions.code` stays the contract. */
    private fun ErrorCode.toErrorType(): ErrorType = when (this) {
        ErrorCode.NOT_FOUND -> ErrorType.NOT_FOUND
        ErrorCode.VALIDATION, ErrorCode.CONFLICT -> ErrorType.BAD_REQUEST
        ErrorCode.UNAVAILABLE, ErrorCode.INTERNAL -> ErrorType.INTERNAL_ERROR
    }
}
