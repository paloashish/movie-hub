package com.moviehub.movie.api.http

import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import com.moviehub.movie.common.RestExceptionHandler
import com.moviehub.movie.service.PosterService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.multipart.MaxUploadSizeExceededException

@WebMvcTest(controllers = [PosterController::class])
@Import(RestExceptionHandler::class, PosterControllerTest.Mocks::class)
class PosterControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val posterService: PosterService,
) {

    @TestConfiguration
    class Mocks {
        @Bean fun posterService(): PosterService = mockk()
    }

    private val png = MockMultipartFile("file", "poster.png", "image/png", ByteArray(10))

    @Test
    fun `should answer the new poster url when the upload succeeds`() {
        every { posterService.store(7, any()) } returns "/posters/7.png"

        mockMvc.perform(multipart("/movies/7/poster").file(png))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.posterUrl").value("/posters/7.png"))
    }

    @Test
    fun `should answer 400 with code VALIDATION when the file type is rejected`() {
        every { posterService.store(7, any()) } throws ValidationException("file type must be JPG or PNG")

        mockMvc.perform(multipart("/movies/7/poster").file(png))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION"))
            .andExpect(jsonPath("$.detail").value("file type must be JPG or PNG"))
    }

    @Test
    fun `should answer 404 with code NOT_FOUND when the movie does not exist`() {
        every { posterService.store(99, any()) } throws NotFoundException("Movie", 99)

        mockMvc.perform(multipart("/movies/99/poster").file(png))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    fun `should answer 400 when the file part is missing`() {
        mockMvc.perform(multipart("/movies/7/poster"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION"))
    }

    @Test
    fun `should answer 400 with code VALIDATION naming the limit when the upload exceeds the multipart limit`() {
        // Spring raises this before the controller runs for files over spring.servlet.multipart.max-file-size
        every { posterService.store(7, any()) } throws MaxUploadSizeExceededException(10L * 1024 * 1024)

        mockMvc.perform(multipart("/movies/7/poster").file(png))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION"))
            .andExpect(jsonPath("$.detail").value("file must be at most 10 MB"))
    }
}
