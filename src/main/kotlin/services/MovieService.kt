package org.example.interviewtemplate.services

import org.example.interviewtemplate.dto.Movie
import org.example.interviewtemplate.dto.MovieOpinion
import org.example.interviewtemplate.dto.RegisterMovie
import org.example.interviewtemplate.dto.toMovie
import org.example.interviewtemplate.entities.MovieEntity
import org.example.interviewtemplate.entities.MovieOpinionEntity
import org.example.interviewtemplate.repositories.MovieOpinionRepository
import org.example.interviewtemplate.repositories.MovieRepository
import org.example.interviewtemplate.repositories.UserRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

interface MovieService {
    suspend fun register(input: RegisterMovie): Movie
    suspend fun postOpinion(username: String, movieOpinion: MovieOpinion)
}

@Service
class MovieServiceImpl(
    private val movieRepository: MovieRepository,
    private val userRepository: UserRepository,
    private val movieOpinionRepository: MovieOpinionRepository
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

    override suspend fun postOpinion(username: String, movieOpinion: MovieOpinion) {
        val movie = movieRepository.findByTitle(movieOpinion.title)
            ?: throw IllegalArgumentException("This title:${movieOpinion.title} does not exists.")
        val user = userRepository.findByName(username)
            ?: throw AuthenticationException("User with username:$username does not exists.")
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
            TODO("inc-dec movie likes..")
            return
        }
        // At this point, we know user has already voted for this movie.
        if (voted.opinion == movieOpinion.opinion) {
            throw IllegalArgumentException(
                "You have already voted for this movie with:${movieOpinion.opinion}." +
                        "Users can only vote once, and after that they can only swap" +
                        " the vote or retract it"
            )
        }
        movieOpinionRepository.updateOpinion(opinion)
        TODO("inc-dec movie likes..")
    }
}