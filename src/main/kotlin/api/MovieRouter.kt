package org.example.interviewtemplate.api

import kotlinx.coroutines.reactor.awaitSingle
import org.example.interviewtemplate.api.utils.awaitReceive
import org.example.interviewtemplate.api.utils.pathVariableOrNull
import org.example.interviewtemplate.config.AuthenticationToken
import org.example.interviewtemplate.dto.AuthenticatedUser
import org.example.interviewtemplate.dto.MovieOpinion
import org.example.interviewtemplate.dto.RegisterMovie
import org.example.interviewtemplate.services.AuthenticationException
import org.example.interviewtemplate.services.MovieService
import org.example.interviewtemplate.util.logger
import org.example.interviewtemplate.util.toMono
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import java.security.Principal

@Configuration
class MovieRouter(private val movieHandler: MovieHandler) {

    @Bean
    fun movieRoutes() = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            POST("api/movies", movieHandler::registerMovie)
            GET("api/movies/{title}", movieHandler::findMovieByTitle)
            GET("api/movies", movieHandler::findAllMovies)
            GET("api/movies/{user}/all", movieHandler::findAllMoviesByUser)
            POST("api/movies/opinion", movieHandler::postOpinion)
            GET("api/movies/opinions/all", movieHandler::findAllOpinionsForUser)
            POST("api/movies/opinion/retract", movieHandler::retractOpinion)
        }
    }
}

@Component
class MovieHandler(private val movieService: MovieService) {
    private val logger = logger()

    suspend fun registerMovie(request: ServerRequest): ServerResponse {
        logger.info("request: POST api/movies")
        val movie = request.awaitReceive<RegisterMovie>()
        val registeredMovie = movieService.register(movie)
        return ServerResponse
            .status(201)
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

    suspend fun findAllMovies(request: ServerRequest): ServerResponse {
        logger.info("request: GET api/movies/")
        val movies = movieService.findAll()
        return ServerResponse.ok().bodyValueAndAwait(movies)
    }

    suspend fun findAllMoviesByUser(request: ServerRequest): ServerResponse {
        logger.info("request: GET api/movies/{user}/all")
        val target = request.pathVariableOrNull("user")
        requireNotNull(target) { "User id is missing from path." }
        val userId = target.toIntOrNull()
        requireNotNull(userId) { "Provided user-id is not convertable into an integer." }
        val moviesByUser = movieService.findAllMoviesByUser(userId)
        return ServerResponse.ok().bodyValueAndAwait(moviesByUser)
    }

    suspend fun postOpinion(request: ServerRequest): ServerResponse {
        logger.info("request: POST api/movies/opinion")
        val movieOpinion = request.awaitReceive<MovieOpinion>()
        val principal = request.awaitPrincipal()
            ?: throw AuthenticationException("Principal is missing.")
        if (principal.name.isNullOrBlank()) {
            throw AuthenticationException("Username in principal is missing.")
        }
        val userId = retrieveUserId(principal)
        val user = AuthenticatedUser(principal.name, userId)
        movieService.postOpinion(user, movieOpinion)
        return ServerResponse.ok().buildAndAwait()
    }

    suspend fun findAllOpinionsForUser(request: ServerRequest): ServerResponse {
        logger.info("request: GET api/movies/opinion")
        val principal = request.awaitPrincipal()
            ?: throw AuthenticationException("Principal is missing.")
        if (principal.name.isNullOrBlank()) {
            throw AuthenticationException("Username in principal is missing.")
        }
        val userId = retrieveUserId(principal)
        val user = AuthenticatedUser(principal.name, userId)
        val opinions = movieService.findAllOpinionsByUser(user)
        return ServerResponse.ok().bodyValueAndAwait(opinions)
    }

    private suspend inline fun <reified T : Any> ServerResponse.BodyBuilder.bodyValueAndAwait(body: T): ServerResponse =
        body(body.toMono(), object : ParameterizedTypeReference<T>() {}).awaitSingle()

    suspend fun retractOpinion(request: ServerRequest): ServerResponse {
        logger.info("request: api/movies/opinion/retract")
        val movieOpinion = request.awaitReceive<MovieOpinion>()
        val principal = request.awaitPrincipal()
            ?: throw AuthenticationException("Principal is missing.")
        if (principal.name.isNullOrBlank()) {
            throw AuthenticationException("Username in principal is missing.")
        }
        val userId = retrieveUserId(principal)
        val user = AuthenticatedUser(principal.name, userId)
        movieService.removeOpinionForMovie(user, movieOpinion)
        return ServerResponse.ok().buildAndAwait()
    }

    private fun retrieveUserId(principal: Principal): Int {
        val authenticationToken = principal as? AuthenticationToken
        checkNotNull(authenticationToken) { "Principal cannot be casted to `AuthenticationToken`." }
        val principalWithUserId = authenticationToken.principalWithUserId
        checkNotNull(principalWithUserId) { "Principal is not authenticated." }
        return principalWithUserId.userId
    }
}