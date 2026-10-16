package com.moviehub.movie.api.http

import com.moviehub.movie.service.PosterService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/** The one REST endpoint (ADR 0003): multipart so the browser can report upload progress. No logic here. */
@RestController
class PosterController(private val posterService: PosterService) {

    data class PosterResponse(val posterUrl: String)

    @PostMapping("/movies/{id}/poster", consumes = ["multipart/form-data"])
    fun upload(@PathVariable id: Long, @RequestParam("file") file: MultipartFile): PosterResponse =
        PosterResponse(posterService.store(id, file))
}
