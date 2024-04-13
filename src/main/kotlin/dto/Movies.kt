package org.example.interviewtemplate.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.interviewtemplate.entities.MovieEntity

@Serializable
data class RegisterMovie(
    val title: String,
    val description: String,
    @SerialName("user_id")
    val userId: Int
)

@Serializable
data class Movie(
    val title: String,
    val description: String,
    @SerialName("user_id")
    val userId: Int,
    /**
     * Represents the date it was registered.
     */
    val date: String,
    val likes: Int,
    val hates: Int
)

fun MovieEntity.toMovie(): Movie {
    return Movie(
        title = title,
        description = description,
        userId = userId,
        date = date,
        likes = likes,
        hates = hates
    )
}

@Serializable
data class MovieOpinion(
    val title: String,
    val opinion: Opinion
)

@Serializable
enum class Opinion {
    LIKE,
    HATE
}

@Serializable
data class MovieWithUser(
    val id: Int,
    val title: String,
    val description: String,
    @SerialName("posted_by_user")
    val user: User,
    /**
     * Represents the date it was registered.
     */
    val date: String,
    val likes: Int,
    val hates: Int
)

@Serializable
data class UserMovieOpinion(
    val opinion: Opinion,
    @SerialName("movie_id")
    val movieId: Int
)