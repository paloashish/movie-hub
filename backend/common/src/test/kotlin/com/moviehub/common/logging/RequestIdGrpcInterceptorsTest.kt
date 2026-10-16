package com.moviehub.common.logging

import com.moviehub.contracts.people.v1.ListPeopleRequest
import com.moviehub.contracts.people.v1.PeopleList
import com.moviehub.contracts.people.v1.PeopleServiceGrpc
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class RequestIdGrpcInterceptorsTest {

    private val method = PeopleServiceGrpc.getListPeopleMethod()

    @Test
    fun `client interceptor should copy the MDC request id into the call metadata`() {
        val call = mockk<ClientCall<ListPeopleRequest, PeopleList>>(relaxed = true)
        val channel = mockk<Channel> { every { newCall(method, any()) } returns call }
        val headers = slot<Metadata>()

        RequestId.withId("req-9") {
            RequestIdClientInterceptor().interceptCall(method, CallOptions.DEFAULT, channel).start(mockk(relaxed = true), Metadata())
        }

        verify { call.start(any(), capture(headers)) }
        assertEquals("req-9", headers.captured.get(RequestId.METADATA_KEY))
    }

    @Test
    fun `client interceptor should send no header when there is no request id in MDC`() {
        val call = mockk<ClientCall<ListPeopleRequest, PeopleList>>(relaxed = true)
        val channel = mockk<Channel> { every { newCall(method, any()) } returns call }
        val headers = slot<Metadata>()

        RequestIdClientInterceptor().interceptCall(method, CallOptions.DEFAULT, channel).start(mockk(relaxed = true), Metadata())

        verify { call.start(any(), capture(headers)) }
        assertNull(headers.captured.get(RequestId.METADATA_KEY))
    }

    @Test
    fun `server interceptor should expose the incoming id in MDC while the handler and its callbacks run`() {
        val call = mockk<ServerCall<ListPeopleRequest, PeopleList>>(relaxed = true) { every { methodDescriptor } returns method }
        val headers = Metadata().apply { put(RequestId.METADATA_KEY, "req-9") }
        var seenInHandler: String? = null
        var seenInCallback: String? = null
        val innerListener = mockk<ServerCall.Listener<ListPeopleRequest>>(relaxed = true) {
            every { onHalfClose() } answers { seenInCallback = MDC.get(RequestId.MDC_KEY) }
        }
        val handler = mockk<ServerCallHandler<ListPeopleRequest, PeopleList>> {
            every { startCall(any(), any()) } answers { seenInHandler = MDC.get(RequestId.MDC_KEY); innerListener }
        }

        val listener = RequestIdServerInterceptor().interceptCall(call, headers, handler)
        listener.onHalfClose()

        assertEquals("req-9", seenInHandler)
        assertEquals("req-9", seenInCallback)
        assertNull(MDC.get(RequestId.MDC_KEY))
    }

    @Test
    fun `server interceptor should generate an id when the metadata has none`() {
        val call = mockk<ServerCall<ListPeopleRequest, PeopleList>>(relaxed = true) { every { methodDescriptor } returns method }
        var seenInHandler: String? = null
        val handler = mockk<ServerCallHandler<ListPeopleRequest, PeopleList>> {
            every { startCall(any(), any()) } answers { seenInHandler = MDC.get(RequestId.MDC_KEY); mockk(relaxed = true) }
        }

        RequestIdServerInterceptor().interceptCall(call, Metadata(), handler)

        assertNotNull(seenInHandler)
    }
}
