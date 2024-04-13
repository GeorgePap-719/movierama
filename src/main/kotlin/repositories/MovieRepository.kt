package org.example.interviewtemplate.repositories

import org.example.interviewtemplate.dto.Opinion
import org.example.interviewtemplate.entities.MovieEntity
import org.example.interviewtemplate.repositories.orm.mapToMovieEntities
import org.example.interviewtemplate.repositories.orm.mapToMovieEntity
import org.example.interviewtemplate.repositories.util.checkForSingleRowUpdate
import org.example.interviewtemplate.repositories.util.saveAndReturnGeneratedId
import org.example.interviewtemplate.util.debug
import org.example.interviewtemplate.util.logger
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.r2dbc.core.bind
import org.springframework.stereotype.Repository

interface MovieRepository {
    suspend fun save(input: MovieEntity): MovieEntity
    suspend fun findByTitle(target: String): MovieEntity?
    suspend fun findAll(): List<MovieEntity>
    suspend fun findAllByUser(target: Int): List<MovieEntity>
    suspend fun postOpinionByMovieId(target: Int, newOpinion: Opinion): Int
    suspend fun updateOpinionByMovieId(target: Int, newOpinion: Opinion): Int
    suspend fun deleteAll(): Int
}

@Repository
class MovieRepositoryImpl(private val template: R2dbcEntityTemplate) : MovieRepository {
    private val logger = logger()

    override suspend fun save(input: MovieEntity): MovieEntity {
        check(input.id == 0) { "All ids should be zero, they are auto-increased by db." }
        logger.debug { "Saving movie:$input." }
        val spec = template.databaseClient.sql {
            //language=MySQL
            """INSERT INTO movierama.movies (title, description, user_id, date, likes, hates) 
                values (?, ?, ?, ?, ?, ?)
            """.trimMargin()
        }
            .bind<String>(0, input.title)
            .bind<String>(1, input.description)
            .bind<Int>(2, input.userId)
            .bind<String>(3, input.date)
            .bind<Int>(4, input.likes)
            .bind<Int>(5, input.hates)
        val generatedId = spec.saveAndReturnGeneratedId()
        val newMovie = input.copy(id = generatedId)
        return newMovie
    }

    override suspend fun findByTitle(target: String): MovieEntity? {
        val spec = template.databaseClient.sql {
            //language=MySQL
            "SELECT * FROM movierama.movies where title='$target'"
        }
        return mapToMovieEntity(spec)
    }

    override suspend fun findAll(): List<MovieEntity> {
        val spec = template.databaseClient.sql {
            //language=MySQL
            "SELECT * FROM movierama.movies"
        }
        return mapToMovieEntities(spec)
    }

    override suspend fun findAllByUser(target: Int): List<MovieEntity> {
        val spec = template.databaseClient.sql {
            //language=MySQL
            "SELECT * FROM movierama.movies WHERE user_id=$target"
        }
        return mapToMovieEntities(spec)
    }

    override suspend fun postOpinionByMovieId(target: Int, newOpinion: Opinion): Int {
        val spec = when (newOpinion) {
            Opinion.LIKE -> {
                template.databaseClient.sql {
                    //language=MySQL
                    """
                UPDATE movierama.movies 
                set likes=likes + 1 
                where id=$target
                """.trimIndent()
                }
            }

            Opinion.HATE -> {
                template.databaseClient.sql {
                    //language=MySQL
                    """
                UPDATE movierama.movies 
                set hates=hates + 1
                where id=$target
                """.trimIndent()
                }
            }
        }
        val updatedRows = spec.fetch().awaitRowsUpdated()
        checkForSingleRowUpdate(updatedRows)
        return updatedRows.toInt()
    }

    override suspend fun updateOpinionByMovieId(target: Int, newOpinion: Opinion): Int {
        val spec = when (newOpinion) {
            Opinion.LIKE -> {
                template.databaseClient.sql {
                    //language=MySQL
                    """
                UPDATE movierama.movies 
                set likes=likes + 1, hates=hates - 1 
                where id=$target
                """.trimIndent()
                }
            }

            Opinion.HATE -> {
                template.databaseClient.sql {
                    //language=MySQL
                    """
                UPDATE movierama.movies 
                set likes=likes - 1, hates=hates + 1
                where id=$target
                """.trimIndent()
                }
            }
        }
        val updatedRows = spec.fetch().awaitRowsUpdated()
        checkForSingleRowUpdate(updatedRows)
        return updatedRows.toInt()
    }

    override suspend fun deleteAll(): Int {
        logger.debug { "Deleting all movies." }
        val spec = template.databaseClient.sql {
            //language=MySQL
            "DELETE FROM movierama.movies"
        }
        return spec.fetch().awaitRowsUpdated().toInt()
    }
}