package com.moviehub.common.logging

import io.grpc.Metadata
import org.slf4j.MDC
import java.util.UUID

/**
 * One request ID follows a user action through movie-service and into people-service (application-architecture 4.3).
 * It travels as the HTTP header X-Request-Id, then as gRPC metadata x-request-id, and is logged from MDC in both services.
 */
object RequestId {
    const val HEADER = "X-Request-Id"
    const val MDC_KEY = "requestId"
    private const val MAX_LENGTH = 64

    val METADATA_KEY: Metadata.Key<String> = Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER)

    /** Short, unique enough for log correlation; not a security token. */
    fun generate(): String = UUID.randomUUID().toString().substring(0, 8)

    /** Accepts an incoming ID only if it looks sane; otherwise a new one is generated. */
    fun sanitize(incoming: String?): String =
        incoming?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_LENGTH && it.all { c -> c.isLetterOrDigit() || c == '-' } }
            ?: generate()

    /** Runs [block] with the ID in MDC, restoring whatever was there before. */
    inline fun <T> withId(id: String, block: () -> T): T {
        val previous = MDC.get(MDC_KEY)
        MDC.put(MDC_KEY, id)
        try {
            return block()
        } finally {
            if (previous == null) MDC.remove(MDC_KEY) else MDC.put(MDC_KEY, previous)
        }
    }
}
