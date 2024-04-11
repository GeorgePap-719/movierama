package org.example.interviewtemplate.repositories

import org.example.interviewtemplate.dto.Opinion
import org.example.interviewtemplate.entities.MovieOpinionEntity
import org.example.interviewtemplate.repositories.orm.mapToMovieOpinionEntities
import org.example.interviewtemplate.repositories.util.checkForDoubleRowUpdate
import org.example.interviewtemplate.repositories.util.checkForSingleRowUpdate
import org.example.interviewtemplate.repositories.util.saveAndReturnGeneratedId
import org.example.interviewtemplate.util.debug
import org.example.interviewtemplate.util.logger
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.r2dbc.core.bind
import org.springframework.stereotype.Repository

interface MovieOpinionRepository {
    suspend fun save(input: MovieOpinionEntity): MovieOpinionEntity
    suspend fun updateOpinion(input: MovieOpinionEntity): Int
    suspend fun findAllOpinionsByUser(target: Int): List<MovieOpinionEntity>
    suspend fun deleteOpinionByMovieId(target: Int, opinion: Opinion)
    suspend fun deleteById(target: Int)
    suspend fun deleteAll(): Int
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

    override suspend fun updateOpinion(input: MovieOpinionEntity): Int {
        val spec = template.databaseClient.sql {
            //language=MySQL
            """
                UPDATE movierama.opinions 
                set opinion='${input.opinion}' 
                where user_id=${input.userId} and movie_id=${input.movieId}
                """.trimIndent()
        }
        val updatedRows = spec.fetch().awaitRowsUpdated()
        checkForSingleRowUpdate(updatedRows)
        return updatedRows.toInt()
    }

    override suspend fun findAllOpinionsByUser(target: Int): List<MovieOpinionEntity> {
        val spec = template.databaseClient.sql {
            //language=MySQL
            "SELECT * FROM movierama.opinions where user_id=$target"
        }
        return mapToMovieOpinionEntities(spec)
    }

    override suspend fun deleteOpinionByMovieId(target: Int, opinion: Opinion) {
        logger.debug { "Deleting movie with id:$target." }
        val column = if (Opinion.HATE == opinion) "hates" else "likes"
        val spec = template.databaseClient.sql {
            //language=MySQL
            """DELETE FROM movierama.opinions where movie_id=$target;
                UPDATE movierama.movies
                SET $column=$column - 1
                where id=$target
            """.trimMargin()
        }
        val updatedRows = spec.fetch().awaitRowsUpdated()
        checkForDoubleRowUpdate(updatedRows)
    }

    override suspend fun deleteById(target: Int) {
        logger.debug { "Deleting opinion with id:$target." }
        val spec = template.databaseClient.sql {
            //language=MySQL
            "DELETE FROM movierama.opinions where id=$target"
        }
        val updatedRows = spec.fetch().awaitRowsUpdated()
        checkForSingleRowUpdate(updatedRows)
    }

    override suspend fun deleteAll(): Int {
        logger.debug { "Deleting all opinions." }
        val spec = template.databaseClient.sql {
            //language=MySQL
            "DELETE FROM movierama.opinions"
        }
        return spec.fetch().awaitRowsUpdated().toInt()
    }
}

