package org.example.interviewtemplate.services

import org.example.interviewtemplate.dto.*
import org.example.interviewtemplate.entities.MovieEntity
import org.example.interviewtemplate.entities.MovieOpinionEntity
import org.example.interviewtemplate.repositories.MovieOpinionRepository
import org.example.interviewtemplate.repositories.MovieRepository
import org.example.interviewtemplate.repositories.UserRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface MovieService {
    suspend fun register(input: RegisterMovie): Movie
    suspend fun postOpinion(user: AuthenticatedUser, movieOpinion: MovieOpinion)
    suspend fun removeOpinionForMovie(user: AuthenticatedUser, movieOpinion: MovieOpinion)
    suspend fun findAllOpinionsByUser(user: AuthenticatedUser): List<UserMovieOpinion>
    suspend fun findMovieByTitle(target: String): Movie?
    suspend fun findAll(): List<MovieWithUser>
}

@Service
class MovieServiceImpl(
    private val movieRepository: MovieRepository,
    private val movieOpinionRepository: MovieOpinionRepository,
    private val userRepository: UserRepository
) : MovieService {
    override suspend fun register(input: RegisterMovie): Movie {
        val movie = MovieEntity(
            title = input.title,
            description = input.description,
            userId = input.userId,
            date = System.currentTimeMillis().toString(),
            likes = 0,
            hates = 0
        )
        return trySave(movie).toMovie()
    }

    private suspend fun trySave(input: MovieEntity): MovieEntity {
        try {
            return movieRepository.save(input)
        } catch (e: DuplicateKeyException) {
            throw IllegalArgumentException("The movie with title:${input.title} already exists.")
        }
    }

    @Transactional
    override suspend fun postOpinion(user: AuthenticatedUser, movieOpinion: MovieOpinion) {
        val movie = movieRepository.findByTitle(movieOpinion.title)
            ?: throw IllegalArgumentException("This title:${movieOpinion.title} does not exists.")
        if (user.id == movie.userId) {
            throw IllegalArgumentException("A user cannot post an opinion for a movie he posted.")
        }
        val opinions = movieOpinionRepository.findAllOpinionsByUser(user.id)
        val voted = opinions.find { it.movieId == movie.id }
        val opinion = MovieOpinionEntity(
            opinion = movieOpinion.opinion,
            userId = user.id,
            movieId = movie.id
        )
        if (voted == null) {
            // Fast-path: save opinion.
            movieOpinionRepository.save(opinion)
            movieRepository.postOpinionByMovieId(movie.id, movieOpinion.opinion)
            return
        }
        // At this point, we know user has already voted for this movie.
        if (voted.opinion == movieOpinion.opinion) {
            throw IllegalArgumentException(
                "You have already voted for this movie with:${movieOpinion.opinion}." +
                        " Users can only vote once, and after that they can only swap" +
                        " their vote or retract it"
            )
        }
        movieOpinionRepository.updateOpinion(opinion)
        movieRepository.updateOpinionByMovieId(movie.id, movieOpinion.opinion)
    }

    override suspend fun removeOpinionForMovie(
        user: AuthenticatedUser,
        movieOpinion: MovieOpinion
    ) {
        val movie = movieRepository.findByTitle(movieOpinion.title)
            ?: throw IllegalArgumentException("This title:${movieOpinion.title} does not exists.")
        val opinions = movieOpinionRepository.findAllOpinionsByUser(user.id)
        val voted = opinions.find { it.movieId == movie.id }
        if (voted == null) {
            throw IllegalArgumentException("Users can only retract their vote.")
        }
        movieOpinionRepository.deleteOpinionByMovieId(movie.id, movieOpinion.opinion)
    }

    override suspend fun findAllOpinionsByUser(user: AuthenticatedUser): List<UserMovieOpinion> {
        return movieOpinionRepository
            .findAllOpinionsByUser(user.id)
            .map { UserMovieOpinion(it.opinion, it.movieId) }
    }

    override suspend fun findMovieByTitle(target: String): Movie? {
        val entity = movieRepository.findByTitle(target) ?: return null
        return entity.toMovie()
    }

    override suspend fun findAll(): List<MovieWithUser> {
        val movies = movieRepository.findAll()
        val ids = movies.map { it.userId }
        val storedUsers = userRepository.findAllById(ids)
        return movies.map { movie ->
            val user = storedUsers.find { it.id == movie.userId } ?: error("unexpected")
            MovieWithUser(
                movie.id,
                movie.title,
                movie.description,
                User(user.name, user.id),
                movie.date,
                movie.likes,
                movie.hates
            )
        }
    }
}