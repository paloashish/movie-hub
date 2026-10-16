package com.moviehub.movie.api.graphql

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * GraphQL `MovieInput`. Bean Validation runs before the resolver body (application-architecture 4.2);
 * violations become a VALIDATION error naming the field, which the form shows under itself.
 */
data class MovieInput(
    @field:NotBlank(message = "must not be blank")
    @field:Size(max = 200, message = "must be at most 200 characters")
    val title: String,

    // The schema's Int! guarantees presence; only the range is checked here
    @field:Min(value = 1888, message = "must be 1888 or later")   // the first film ever
    @field:MaxYearsAhead(5)
    val releaseYear: Int,

    @field:Size(max = 2000, message = "must be at most 2000 characters")
    val synopsis: String? = null,
)
