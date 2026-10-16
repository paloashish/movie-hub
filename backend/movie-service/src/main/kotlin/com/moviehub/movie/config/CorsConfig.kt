package com.moviehub.movie.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** Allowed browser origins, e.g. the SvelteKit dev server. Set in application.yml per profile. */
@ConfigurationProperties("moviehub.cors")
data class CorsProperties(val allowedOrigins: List<String> = emptyList())

/** Lets the frontend on another port call GraphQL and the poster endpoints during local development. */
@Configuration
@EnableConfigurationProperties(CorsProperties::class)   // binds the properties wherever this config is loaded, slices included
class CorsConfig(private val props: CorsProperties) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        if (props.allowedOrigins.isEmpty()) return
        registry.addMapping("/**")
            .allowedOrigins(*props.allowedOrigins.toTypedArray())
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("X-Request-Id")
    }
}
