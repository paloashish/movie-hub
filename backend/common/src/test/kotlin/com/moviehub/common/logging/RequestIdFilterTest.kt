package com.moviehub.common.logging

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class RequestIdFilterTest {

    private val filter = RequestIdFilter()
    private val response = mockk<HttpServletResponse> {
        every { setHeader(any(), any()) } just runs
        every { status } returns 200
    }

    private fun request(header: String?) = mockk<HttpServletRequest> {
        every { getHeader(RequestId.HEADER) } returns header
        every { method } returns "POST"
        every { requestURI } returns "/graphql"
    }

    @Test
    fun `should reuse the incoming X-Request-Id, expose it to the chain via MDC and echo it in the response`() {
        var seenInChain: String? = null
        val chain = mockk<FilterChain> { every { doFilter(any(), any()) } answers { seenInChain = MDC.get(RequestId.MDC_KEY) } }

        filter.doFilter(request("client-42"), response, chain)

        assertEquals("client-42", seenInChain)
        verify { response.setHeader(RequestId.HEADER, "client-42") }
    }

    @Test
    fun `should generate an id when the request has none`() {
        var seenInChain: String? = null
        val chain = mockk<FilterChain> { every { doFilter(any(), any()) } answers { seenInChain = MDC.get(RequestId.MDC_KEY) } }

        filter.doFilter(request(null), response, chain)

        assertNotNull(seenInChain)
        verify { response.setHeader(RequestId.HEADER, seenInChain!!) }
    }

    @Test
    fun `should clear MDC after the request even when the chain throws`() {
        val chain = mockk<FilterChain> { every { doFilter(any(), any()) } throws IllegalStateException("boom") }

        runCatching { filter.doFilter(request("x"), response, chain) }

        assertNull(MDC.get(RequestId.MDC_KEY))
    }
}
