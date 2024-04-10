package org.example.interviewtemplate.entities

import org.example.interviewtemplate.dto.Opinion

data class MovieOpinionEntity(
    val id: Int = 0,
    val opinion: Opinion,
    val userId: Int,
    val movieId: Int
)