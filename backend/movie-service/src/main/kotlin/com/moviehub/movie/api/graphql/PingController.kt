package com.moviehub.movie.api.graphql

import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

/** Smallest possible query, used to prove the GraphQL wiring before any story exists. */
@Controller
class PingController {

    @QueryMapping
    fun ping(): String = "pong"
}
