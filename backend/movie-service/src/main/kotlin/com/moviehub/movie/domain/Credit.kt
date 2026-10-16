package com.moviehub.movie.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * A person's role on a movie. personId is a plain value: the person lives in people-service and is looked up
 * over gRPC when the credits are shown (ADR 0001).
 */
@Entity
@Table(name = "credit")
class Credit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "movie_id", nullable = false)
    val movieId: Long,

    @Column(name = "person_id", nullable = false)
    val personId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: Role,
)
