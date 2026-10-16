package com.moviehub.people.config

import com.moviehub.common.logging.RequestIdFilter
import com.moviehub.common.logging.RequestIdServerInterceptor
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.grpc.server.GlobalServerInterceptor

/** Wires the shared request-ID pieces: the gRPC server interceptor (business traffic) and the HTTP filter (actuator). */
@Configuration
class RequestIdConfig {

    @Bean
    @GlobalServerInterceptor
    fun requestIdServerInterceptor(): RequestIdServerInterceptor = RequestIdServerInterceptor()

    @Bean
    fun requestIdFilter(): FilterRegistrationBean<RequestIdFilter> =
        FilterRegistrationBean(RequestIdFilter()).apply { order = Ordered.HIGHEST_PRECEDENCE }
}
