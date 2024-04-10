package org.example.interviewtemplate.api

import org.example.interviewtemplate.dto.RegisterMovie
import org.example.interviewtemplate.services.MovieService
import org.example.interviewtemplate.util.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class MovieRouter(private val movieHandler: MovieHandler) {

    @Bean
    fun movieRoutes() = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            POST("api/movies", movieHandler::registerMovie)
            GET("api/movies/{title}", movieHandler::findMovieByTitle)
        }
    }
}

@Component
class MovieHandler(private val movieService: MovieService) {
    private val logger = logger()

    suspend fun registerMovie(request: ServerRequest): ServerResponse {
        logger.info("request: api/movies")
        val movie = request.awaitReceive<RegisterMovie>()
        val registeredMovie = movieService.register(movie)
        return ServerResponse
            //TODO:.created()
            .status(201)
            .bodyValueAndAwait(registeredMovie)
    }

    suspend fun findMovieByTitle(request: ServerRequest): ServerResponse {
        logger.info("request: api/movies/{title}")
        TODO()
    }
}