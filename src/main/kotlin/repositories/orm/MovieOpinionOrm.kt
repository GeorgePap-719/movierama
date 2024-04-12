package org.example.interviewtemplate.repositories.orm

import kotlinx.coroutines.flow.toList
import org.example.interviewtemplate.dto.Opinion
import org.example.interviewtemplate.entities.MovieOpinionEntity
import org.example.interviewtemplate.repositories.util.getColumn
import org.example.interviewtemplate.util.nullableAsFlow
import org.springframework.r2dbc.core.DatabaseClient

suspend fun mapToMovieOpinionEntities(
    spec: DatabaseClient.GenericExecuteSpec
): List<MovieOpinionEntity> {
    val fetchSpec = spec.map { row, metadata ->
        buildMovieOpinion {
            check(metadata.columnMetadatas.size == 4) {
                "Size should be equal of `MovieOpinionEntity` fields, but got:${metadata.columnMetadatas.size}."
            }
            id = row.getColumn<Int>("id")
            opinion = Opinion.valueOfOrThrow(row.getColumn<String>("opinion"))
            userId = row.getColumn<Int>("user_id")
            movieId = row.getColumn<Int>("movie_id")
        }
    }
    return fetchSpec.all().nullableAsFlow().toList()
}

private fun Opinion.Companion.valueOfOrThrow(input: String): Opinion {
    return try {
        Opinion.valueOf(input)
    } catch (e: IllegalArgumentException) {
        // At this point, if db has saved an invalid value,
        // we have to treat it as an error state.
        throw IllegalStateException(e)
    }
}

private inline fun buildMovieOpinion(action: MovieOpinionBuilder.() -> Unit): MovieOpinionEntity? {
    val builder = MovieOpinionBuilder()
    builder.action()
    return builder.build()
}

private class MovieOpinionBuilder {
    var id: Int? = null
    var opinion: Opinion? = null
    var userId: Int? = null
    var movieId: Int? = null

    val isEmpty: Boolean
        get() = id == null &&
                opinion == null &&
                userId == null &&
                movieId == null

    fun build(): MovieOpinionEntity? {
        if (isEmpty) return null
        val id = requireNotNull(id)
        val opinion = requireNotNull(opinion)
        val userId = requireNotNull(userId)
        val movieId = requireNotNull(movieId)
        return MovieOpinionEntity(
            id = id,
            opinion = opinion,
            userId = userId,
            movieId = movieId
        )
    }
}