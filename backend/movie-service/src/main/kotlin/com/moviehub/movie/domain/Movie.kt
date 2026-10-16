package com.moviehub.movie.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/** A catalogue entry. The poster is a file on disk; only its URL lives here (ADR 0003). */
@Entity
@Table(name = "movie")
class Movie(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(name = "release_year", nullable = false)
    var releaseYear: Int,

    @Column(length = 2000)
    var synopsis: String? = null,

    @Column(name = "poster_url", length = 500)
    var posterUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
