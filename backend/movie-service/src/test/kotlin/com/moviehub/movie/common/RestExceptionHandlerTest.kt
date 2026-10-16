package com.moviehub.movie.common

import com.moviehub.common.error.ConflictException
import com.moviehub.common.error.DependencyUnavailableException
import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(controllers = [RestExceptionHandlerTest.ThrowingController::class])
@Import(RestExceptionHandler::class, RestExceptionHandlerTest.ThrowingController::class)
class RestExceptionHandlerTest(@Autowired private val mockMvc: MockMvc) {

    @RestController
    class ThrowingController {
        @GetMapping("/t/not-found") fun notFound(): String = throw NotFoundException("Movie", 42)
        @GetMapping("/t/validation") fun validation(): String = throw ValidationException("file type must be JPG or PNG")
        @GetMapping("/t/conflict") fun conflict(): String = throw ConflictException("already credited")
        @GetMapping("/t/unavailable") fun unavailable(): String = throw DependencyUnavailableException("people-service")
        @GetMapping("/t/unexpected") fun unexpected(): String = throw IllegalStateException("disk full")
    }

    @Test
    fun `should return 404 problem detail with code NOT_FOUND when not found`() {
        mockMvc.perform(get("/t/not-found"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.detail").value("Movie 42 not found"))
    }

    @Test
    fun `should return 400 with code VALIDATION when validation fails`() {
        mockMvc.perform(get("/t/validation"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION"))
    }

    @Test
    fun `should return 409 with code CONFLICT when a duplicate is rejected`() {
        mockMvc.perform(get("/t/conflict"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CONFLICT"))
    }

    @Test
    fun `should return 503 with code UNAVAILABLE when a dependency is down`() {
        mockMvc.perform(get("/t/unavailable"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("UNAVAILABLE"))
    }

    @Test
    fun `should return 404 with code NOT_FOUND when the URL does not exist`() {
        mockMvc.perform(get("/no/such/path"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    fun `should keep Spring's 405 status with code VALIDATION when the method is not allowed`() {
        mockMvc.perform(post("/t/not-found"))
            .andExpect(status().isMethodNotAllowed)
            .andExpect(jsonPath("$.code").value("VALIDATION"))
    }

    @Test
    fun `should return 500 with a generic message when an unexpected exception is thrown`() {
        mockMvc.perform(get("/t/unexpected"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("INTERNAL"))
            .andExpect(jsonPath("$.detail").value("Something went wrong"))
    }
}
