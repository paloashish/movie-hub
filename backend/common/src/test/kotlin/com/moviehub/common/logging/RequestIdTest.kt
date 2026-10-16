package com.moviehub.common.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class RequestIdTest {

    @Test
    fun `should keep a sane incoming id`() {
        assertEquals("abc-123", RequestId.sanitize("abc-123"))
    }

    @Test
    fun `should generate a new id when the incoming one is missing, blank, too long or contains odd characters`() {
        assertNotEquals("", RequestId.sanitize(null))
        assertNotEquals("   ", RequestId.sanitize("   "))
        assertNotEquals("x".repeat(65), RequestId.sanitize("x".repeat(65)))
        assertNotEquals("evil\nvalue", RequestId.sanitize("evil\nvalue"))
    }

    @Test
    fun `should put the id in MDC only for the duration of the block`() {
        var seen: String? = null

        RequestId.withId("req-1") { seen = MDC.get(RequestId.MDC_KEY) }

        assertEquals("req-1", seen)
        assertNull(MDC.get(RequestId.MDC_KEY))
    }

    @Test
    fun `should restore the previous id after a nested block`() {
        RequestId.withId("outer") {
            RequestId.withId("inner") { assertEquals("inner", MDC.get(RequestId.MDC_KEY)) }
            assertEquals("outer", MDC.get(RequestId.MDC_KEY))
        }
    }
}
