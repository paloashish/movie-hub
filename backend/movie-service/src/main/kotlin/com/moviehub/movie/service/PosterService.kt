package com.moviehub.movie.service

import com.moviehub.common.error.ValidationException
import com.moviehub.movie.config.PostersProperties
import com.moviehub.movie.repository.MovieRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Poster files on local disk (ADR 0003). One file per movie, named by the movie id, so a new upload
 * replaces the old one. The database only keeps the URL.
 */
@Service
class PosterService(
    private val repository: MovieRepository,
    private val movieService: MovieService,
    private val props: PostersProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** E3-S1: validate, write the file, point the movie at it. Returns the new posterUrl. */
    @Transactional
    fun store(movieId: Long, file: MultipartFile): String {
        val extension = EXTENSIONS[file.contentType] ?: throw ValidationException("file type must be JPG or PNG")
        if (file.isEmpty) throw ValidationException("file is empty")
        if (file.size > props.maxSize.toBytes()) throw ValidationException("file must be at most ${props.maxSize.toMegabytes()} MB")
        val movie = movieService.get(movieId)

        // Validation and lookup first, so a bad request never touches the disk
        Files.createDirectories(props.path)
        val fileName = "$movieId.$extension"
        // Write beside the target, then swap in one step: a failure mid-write never leaves a half-written poster,
        // and the old file (same name or not) stays intact until the new one is complete
        val target = props.path.resolve(fileName)
        val temp = Files.createTempFile(props.path, "$movieId-", ".upload")
        try {
            file.inputStream.use { Files.copy(it, temp, StandardCopyOption.REPLACE_EXISTING) }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(temp)
        }
        movie.posterUrl?.let { if (fileNameOf(it) != fileName) deleteFile(it) }

        // E3-S3: the file keeps its name on replace, so the URL carries a version; browsers then fetch the new picture
        val newUrl = "/posters/$fileName?v=${System.currentTimeMillis()}"
        movie.posterUrl = newUrl
        repository.save(movie)
        return newUrl
    }

    /** Removes the file behind a posterUrl, if any. Logs and continues on failure: an orphan file is harmless. */
    fun deleteFile(posterUrl: String) {
        val path: Path = props.path.resolve(fileNameOf(posterUrl))
        try {
            Files.deleteIfExists(path)
        } catch (e: java.io.IOException) {
            log.warn("Could not delete poster file {}", path, e)
        }
    }

    /** "/posters/7.png?v=123" -> "7.png" */
    private fun fileNameOf(posterUrl: String): String = posterUrl.removePrefix("/posters/").substringBefore('?')

    private companion object {
        val EXTENSIONS = mapOf("image/jpeg" to "jpg", "image/png" to "png")
    }
}
