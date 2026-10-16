package com.moviehub.movie.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.util.unit.DataSize
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Path

/**
 * Where poster files live and how big one may be (application-architecture 4.5).
 * `dir` is bound as a String: Spring's Path converter treats "../data/posters" as a resource and rejects it.
 */
@ConfigurationProperties("moviehub.posters")
data class PostersProperties(val dir: String, val maxSize: DataSize = DataSize.ofMegabytes(10)) {
    val path: Path get() = Path.of(dir)
}

/** Serves the poster folder as static files under /posters/, so `posterUrl` is a plain URL (ADR 0003). */
@Configuration
@EnableConfigurationProperties(PostersProperties::class)
class PostersConfig(private val props: PostersProperties) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/posters/**")
            .addResourceLocations(
                props.path.toAbsolutePath().toUri().toString(),
                "classpath:/seed-posters/",   // E6-S2: the sample catalogue's own artwork, replaced by an upload like any poster
            )
    }
}
