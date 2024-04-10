package org.example.interviewtemplate.api

import org.example.interviewtemplate.api.utils.awaitReceive
import org.example.interviewtemplate.api.utils.pathVariableOrNull
import org.example.interviewtemplate.dto.MovieOpinion
import org.example.interviewtemplate.dto.RegisterMovie
import org.example.interviewtemplate.services.AuthenticationException
import org.example.interviewtemplate.services.MovieService
import org.example.interviewtemplate.util.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import java.net.URI

@Configuration
class MovieRouter(private val movieHandler: MovieHandler) {

    @Bean
    fun movieRoutes() = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            POST("api/movies", movieHandler::registerMovie)
            GET("api/movies/{title}", movieHandler::findMovieByTitle)
            POST("api/movies/opinion", movieHandler::postOpinion)
            POST("api/movies/opinion/retract/", movieHandler::retractOpinion)
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
            .created(URI.create("api/movies/${movie.title}"))
            .bodyValueAndAwait(registeredMovie)
    }

    suspend fun findMovieByTitle(request: ServerRequest): ServerResponse {
        logger.info("request: api/movies/{title}")
        val title = request.pathVariableOrNull("title")
        requireNotNull(title) { "Title is missing from path." }
        val movie = movieService.findMovieByTitle(title)
            ?: return ServerResponse.notFound().buildAndAwait()
        return ServerResponse.ok().bodyValueAndAwait(movie)
    }

    suspend fun postOpinion(request: ServerRequest): ServerResponse {
        logger.info("request: api/movies/opinion")
        val movieOpinion = request.awaitReceive<MovieOpinion>()
        val principal = request.awaitPrincipal()
            ?: throw AuthenticationException("Principal is missing.")
        if (principal.name.isNullOrBlank()) {
            throw AuthenticationException("Username in principal is missing.")
        }
        movieService.postOpinion(principal.name, movieOpinion)
        return ServerResponse.ok().buildAndAwait()
    }

    suspend fun retractOpinion(request: ServerRequest): ServerResponse {
        logger.info("request: api/movies/opinion/retract")
        val movieOpinion = request.awaitReceive<MovieOpinion>()
        val principal = request.awaitPrincipal()
            ?: throw AuthenticationException("Principal is missing.")
        if (principal.name.isNullOrBlank()) {
            throw AuthenticationException("Username in principal is missing.")
        }
        movieService.removeOpinionForMovie(principal.name, movieOpinion)
        return ServerResponse.ok().buildAndAwait()
    }
}