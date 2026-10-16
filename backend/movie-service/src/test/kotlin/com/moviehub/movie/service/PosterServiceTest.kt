package com.moviehub.movie.service

import com.moviehub.common.error.NotFoundException
import com.moviehub.common.error.ValidationException
import com.moviehub.movie.config.PostersProperties
import com.moviehub.movie.domain.Movie
import com.moviehub.movie.repository.MovieRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import org.springframework.util.unit.DataSize
import java.nio.file.Files
import java.nio.file.Path

class PosterServiceTest {

    @TempDir
    lateinit var dir: Path

    private val repository = mockk<MovieRepository>()
    private val movieService = mockk<MovieService>()
    private val service by lazy { PosterService(repository, movieService, PostersProperties(dir.toString(), DataSize.ofKilobytes(1))) }

    private fun png(bytes: Int = 10) = MockMultipartFile("file", "poster.png", "image/png", ByteArray(bytes) { 1 })

    @Test
    fun `should write the file named by movie id and save the url on the movie`() {
        val movie = Movie(7, "Alien", 1979)
        every { movieService.get(7) } returns movie
        every { repository.save(movie) } returns movie

        val url = service.store(7, png())

        assertTrue(url.startsWith("/posters/7.png?v="), url)   // versioned so a replacement is not served from cache
        assertEquals(url, movie.posterUrl)
        assertTrue(Files.exists(dir.resolve("7.png")))
        verify(exactly = 1) { repository.save(movie) }
    }

    @Test
    fun `should map a jpeg to the jpg extension`() {
        val movie = Movie(7, "Alien", 1979)
        every { movieService.get(7) } returns movie
        every { repository.save(movie) } returns movie

        val url = service.store(7, MockMultipartFile("file", "p.jpeg", "image/jpeg", ByteArray(10)))

        assertTrue(url.startsWith("/posters/7.jpg?v="), url)
    }

    @Test
    fun `should remove the old file when a replacement has a different extension`() {
        val movie = Movie(7, "Alien", 1979, posterUrl = "/posters/7.jpg?v=1")
        Files.createDirectories(dir)
        Files.write(dir.resolve("7.jpg"), ByteArray(3))
        every { movieService.get(7) } returns movie
        every { repository.save(movie) } returns movie

        service.store(7, png())

        assertFalse(Files.exists(dir.resolve("7.jpg")))
        assertTrue(Files.exists(dir.resolve("7.png")))
    }

    @Test
    fun `should keep the old file when a replacement has the same extension`() {
        val movie = Movie(7, "Alien", 1979, posterUrl = "/posters/7.png?v=1")
        Files.createDirectories(dir)
        Files.write(dir.resolve("7.png"), ByteArray(3))
        every { movieService.get(7) } returns movie
        every { repository.save(movie) } returns movie

        val url = service.store(7, png(10))

        assertEquals(10, Files.size(dir.resolve("7.png")))   // swapped in atomically, never deleted first
        assertEquals(listOf("7.png"), Files.list(dir).use { s -> s.map { it.fileName.toString() }.toList() })   // no temp file left
        assertNotEquals("/posters/7.png?v=1", url)              // a new version, so the browser refetches (E3-S3)
    }

    @Test
    fun `should accept a file exactly at the size limit`() {
        val movie = Movie(7, "Alien", 1979)
        every { movieService.get(7) } returns movie
        every { repository.save(movie) } returns movie

        assertTrue(service.store(7, png(1024)).startsWith("/posters/7.png"))
    }

    @Test
    fun `should reject a file that is not JPG or PNG before touching the disk`() {
        val ex = assertThrows(ValidationException::class.java) {
            service.store(7, MockMultipartFile("file", "x.gif", "image/gif", ByteArray(10)))
        }

        assertEquals("file type must be JPG or PNG", ex.message)
        assertFalse(Files.exists(dir.resolve("7.gif")))
        verify(exactly = 0) { movieService.get(any()) }
    }

    @Test
    fun `should reject an empty file`() {
        assertThrows(ValidationException::class.java) { service.store(7, png(0)) }
    }

    @Test
    fun `should reject a file above the configured size, naming the limit`() {
        val ex = assertThrows(ValidationException::class.java) { service.store(7, png(1025)) }

        assertTrue(requireNotNull(ex.message).contains("MB"), ex.message)
        verify(exactly = 0) { movieService.get(any()) }
    }

    @Test
    fun `should throw NotFoundException and write nothing when the movie does not exist`() {
        every { movieService.get(99) } throws NotFoundException("Movie", 99)

        assertThrows(NotFoundException::class.java) { service.store(99, png()) }
        assertFalse(Files.exists(dir.resolve("99.png")))
    }

    @Test
    fun `should delete the file behind a poster url and ignore a missing one`() {
        Files.createDirectories(dir)
        Files.write(dir.resolve("7.png"), ByteArray(3))

        service.deleteFile("/posters/7.png?v=42")   // the version query is not part of the file name
        service.deleteFile("/posters/8.png")        // never existed: no exception

        assertFalse(Files.exists(dir.resolve("7.png")))
    }
}
