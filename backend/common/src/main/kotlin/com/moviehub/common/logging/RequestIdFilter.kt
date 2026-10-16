package com.moviehub.common.logging

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory

/**
 * HTTP edge: reads or creates the request ID, echoes it in the response, and writes one access-log line per request.
 * Actuator traffic is not logged to keep the console readable.
 */
class RequestIdFilter : Filter {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val http = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        val id = RequestId.sanitize(http.getHeader(RequestId.HEADER))
        httpResponse.setHeader(RequestId.HEADER, id)
        val startNanos = System.nanoTime()

        RequestId.withId(id) {
            try {
                chain.doFilter(request, response)
            } finally {
                if (!http.requestURI.startsWith("/actuator")) {
                    val millis = (System.nanoTime() - startNanos) / 1_000_000
                    log.info("{} {} -> {} in {} ms", http.method, http.requestURI, httpResponse.status, millis)
                }
            }
        }
    }
}
