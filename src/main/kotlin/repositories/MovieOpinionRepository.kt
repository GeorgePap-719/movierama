package org.example.interviewtemplate.repositories

import org.example.interviewtemplate.entities.MovieOpinionEntity
import org.example.interviewtemplate.repositories.orm.mapToMovieOpinionEntities
import org.example.interviewtemplate.repositories.util.saveAndReturnGeneratedId
import org.example.interviewtemplate.util.debug
import org.example.interviewtemplate.util.logger
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.bind
import org.springframework.stereotype.Repository

interface MovieOpinionRepository {
    suspend fun save(input: MovieOpinionEntity): MovieOpinionEntity
    suspend fun updateOpinion(input: MovieOpinionEntity): MovieOpinionEntity
    suspend fun findAllOpinionsByUser(target: Int): List<MovieOpinionEntity>
}

@Repository
class MovieOpinionRepositoryImpl(
    private val template: R2dbcEntityTemplate
) : MovieOpinionRepository {
    private val logger = logger()

    override suspend fun save(input: MovieOpinionEntity): MovieOpinionEntity {
        check(input.id == 0) { "All ids should be zero, they are auto-increased by db." }
        logger.debug { "Saving movie-opinion:$input." }
        val spec = template.databaseClient.sql {
            //language=MySQL
            """
                INSERT INTO movierama.opinions (opinion, user_id, movie_id) 
                VALUES (?, ?, ?) 
            """.trimIndent()
        }
            .bind<String>(0, input.opinion.toString())
            .bind<Int>(1, input.userId)
            .bind<Int>(2, input.movieId)
        val generatedId = spec.saveAndReturnGeneratedId()
        val postedOpinion = input.copy(id = generatedId)
        return postedOpinion
    }

    override suspend fun updateOpinion(input: MovieOpinionEntity): MovieOpinionEntity {
        TODO("Not yet implemented")
    }

    override suspend fun findAllOpinionsByUser(target: Int): List<MovieOpinionEntity> {
        val spec = template.databaseClient.sql {
            //language=MySQL
            "SELECT * FROM movierama.opinions where user_id=$target"
        }
        return mapToMovieOpinionEntities(spec)
    }
}

