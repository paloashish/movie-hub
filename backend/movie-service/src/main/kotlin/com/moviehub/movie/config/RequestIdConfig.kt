package com.moviehub.movie.config

import com.moviehub.common.logging.RequestIdClientInterceptor
import com.moviehub.common.logging.RequestIdFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.grpc.client.GlobalClientInterceptor

/** Wires the shared request-ID pieces: the HTTP filter in, the gRPC client interceptor out. */
@Configuration
class RequestIdConfig {

    @Bean
    fun requestIdFilter(): FilterRegistrationBean<RequestIdFilter> =
        FilterRegistrationBean(RequestIdFilter()).apply { order = Ordered.HIGHEST_PRECEDENCE }

    @Bean
    @GlobalClientInterceptor
    fun requestIdClientInterceptor(): RequestIdClientInterceptor = RequestIdClientInterceptor()
}
