package org.example.interviewtemplate.entities

data class MovieEntity(
    val id: Int = 0,
    val title: String,
    val description: String,
    val userId: Int,
    val date: String,
    val likes: Int,
    val hates: Int
)