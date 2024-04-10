package org.example.interviewtemplate.services

import org.example.interviewtemplate.dto.User
import org.example.interviewtemplate.dto.toUser
import org.example.interviewtemplate.repositories.UserRepository
import org.springframework.stereotype.Service

interface UserService {
    suspend fun findByName(target: String): User?
}

@Service
class UserServiceImpl(private val userRepository: UserRepository) : UserService {
    override suspend fun findByName(target: String): User? {
        require(target.isNotBlank()) { "Target name is blank." }
        return userRepository.findByName(target)?.toUser()
    }
}