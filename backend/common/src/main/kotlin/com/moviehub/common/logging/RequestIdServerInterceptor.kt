package com.moviehub.common.logging

import io.grpc.ForwardingServerCall
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.slf4j.LoggerFactory

/**
 * gRPC server edge (people-service): takes the request ID from metadata (or creates one), keeps it in MDC for every
 * callback of the call, and writes one log line per call with its status and duration.
 * gRPC runs callbacks on pooled threads, so MDC must be set around each callback, not once.
 */
class RequestIdServerInterceptor : ServerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val id = RequestId.sanitize(headers.get(RequestId.METADATA_KEY))
        val startNanos = System.nanoTime()

        val loggingCall = object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            override fun close(status: Status, trailers: Metadata) {
                RequestId.withId(id) {
                    val millis = (System.nanoTime() - startNanos) / 1_000_000
                    log.info("gRPC {} -> {} in {} ms", call.methodDescriptor.fullMethodName, status.code, millis)
                }
                super.close(status, trailers)
            }
        }

        val listener = RequestId.withId(id) { next.startCall(loggingCall, headers) }

        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            override fun onMessage(message: ReqT) = RequestId.withId(id) { super.onMessage(message) }
            override fun onHalfClose() = RequestId.withId(id) { super.onHalfClose() }
            override fun onReady() = RequestId.withId(id) { super.onReady() }
            override fun onCancel() = RequestId.withId(id) { super.onCancel() }
            override fun onComplete() = RequestId.withId(id) { super.onComplete() }
        }
    }
}
