package com.moviehub.common.logging

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import org.slf4j.MDC

/** gRPC client edge (movie-service): copies the current request ID from MDC into the call metadata. */
class RequestIdClientInterceptor : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> =
        object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                MDC.get(RequestId.MDC_KEY)?.let { headers.put(RequestId.METADATA_KEY, it) }
                super.start(responseListener, headers)
            }
        }
}
