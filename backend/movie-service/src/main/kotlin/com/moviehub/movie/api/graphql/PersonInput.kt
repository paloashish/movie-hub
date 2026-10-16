package com.moviehub.movie.api.graphql

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * GraphQL `PersonInput`. Bean Validation runs before the resolver body (application-architecture 4.2);
 * violations become a VALIDATION error naming the field, which the form shows under itself.
 */
data class PersonInput(
    @field:NotBlank(message = "must not be blank")
    @field:Size(max = 200, message = "must be at most 200 characters")
    val name: String,

    @field:Size(max = 2000, message = "must be at most 2000 characters")
    val bio: String? = null,
)
