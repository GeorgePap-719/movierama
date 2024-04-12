package org.example.interviewtemplate.repositories.orm

import kotlinx.coroutines.flow.toList
import org.example.interviewtemplate.entities.MovieEntity
import org.example.interviewtemplate.repositories.util.getColumn
import org.example.interviewtemplate.util.nullableAsFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingleOrNull

suspend fun mapToMovieEntities(spec: DatabaseClient.GenericExecuteSpec): List<MovieEntity> {
    val fetchSpec = spec.map { row, metadata ->
        buildMovie {
            check(metadata.columnMetadatas.size == 7) {
                "Size should be equal of `MovieEntity` fields, but got:${metadata.columnMetadatas.size}."
            }
            id = row.getColumn<Int>("id")
            title = row.getColumn<String>("title")
            description = row.getColumn<String>("description")
            userId = row.getColumn<Int>("user_id")
            date = row.getColumn<String>("date")
            likes = row.getColumn<Int>("likes")
            hates = row.getColumn<Int>("hates")
        }
    }
    return fetchSpec.all().nullableAsFlow().toList()
}

suspend fun mapToMovieEntity(spec: DatabaseClient.GenericExecuteSpec): MovieEntity? {
    return buildMovie {
        val fetchSpec = spec.map { row, metadata ->
            check(metadata.columnMetadatas.size == 7) {
                "Size should be equal of `MovieEntity` fields, but got:${metadata.columnMetadatas.size}."
            }
            id = row.getColumn<Int>("id")
            title = row.getColumn<String>("title")
            description = row.getColumn<String>("description")
            userId = row.getColumn<Int>("user_id")
            date = row.getColumn<String>("date")
            likes = row.getColumn<Int>("likes")
            hates = row.getColumn<Int>("hates")
        }
        fetchSpec.awaitSingleOrNull()
    }
}

private inline fun buildMovie(action: MovieBuilder.() -> Unit): MovieEntity? {
    val builder = MovieBuilder()
    builder.action()
    return builder.build()
}

private class MovieBuilder {
    var id: Int? = null
    var title: String? = null
    var description: String? = null
    var userId: Int? = null
    var date: String? = null
    var likes: Int? = null
    var hates: Int? = null

    val isEmpty: Boolean
        get() = id == null &&
                title == null &&
                description == null &&
                userId == null &&
                date == null &&
                likes == null &&
                hates == null

    fun build(): MovieEntity? {
        if (isEmpty) return null
        val id = requireNotNull(id)
        val title = requireNotNull(title)
        val description = requireNotNull(description)
        val userId = requireNotNull(userId)
        val date = requireNotNull(date)
        val likes = requireNotNull(likes)
        val hates = requireNotNull(hates)
        return MovieEntity(
            id = id,
            title = title,
            description = description,
            userId = userId,
            date = date,
            likes = likes,
            hates = hates
        )
    }
}