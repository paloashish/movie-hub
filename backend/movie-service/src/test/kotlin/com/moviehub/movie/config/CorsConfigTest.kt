package com.moviehub.movie.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsConfigTest(@Autowired private val mockMvc: MockMvc) {

    @Test
    fun `should allow the frontend dev origin on a preflight request`() {
        mockMvc.perform(
            options("/graphql")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
    }

    @Test
    fun `should reject an unknown origin on a preflight request`() {
        mockMvc.perform(
            options("/graphql")
                .header("Origin", "http://evil.example")
                .header("Access-Control-Request-Method", "POST"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should expose the request id header to the browser`() {
        mockMvc.perform(
            post("/graphql").contentType("application/json").content("""{"query":"{ ping }"}""")
                .header("Origin", "http://localhost:5173"),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Expose-Headers", "X-Request-Id"))
    }
}
