package org.example.interviewtemplate.services

import org.example.interviewtemplate.dto.Movie
import org.example.interviewtemplate.dto.MovieOpinion
import org.example.interviewtemplate.dto.RegisterMovie
import org.example.interviewtemplate.dto.toMovie
import org.example.interviewtemplate.entities.MovieEntity
import org.example.interviewtemplate.repositories.MovieRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

interface MovieService {
    suspend fun register(input: RegisterMovie): Movie
    suspend fun postOpinion(username: String, movieOpinion: MovieOpinion)
}

@Service
class MovieServiceImpl(private val movieRepository: MovieRepository) : MovieService {
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

        TODO("Not yet implemented")
    }
}