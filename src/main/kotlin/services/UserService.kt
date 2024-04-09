package org.example.interviewtemplate.services

import org.example.interviewtemplate.config.SecurityConfig
import org.example.interviewtemplate.dto.RegisterUser
import org.example.interviewtemplate.dto.User
import org.example.interviewtemplate.entities.UserEntity
import org.example.interviewtemplate.repositories.UserRepository
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UserService {
    suspend fun register(input: RegisterUser): User
    suspend fun findByName(target: String): User?
}

@Service
class UserServiceImpl(
    private val securityConfig: SecurityConfig,
    private val userRepository: UserRepository
) : UserService {

    @Transactional
    override suspend fun register(input: RegisterUser): User {
        val salt = securityConfig.salt
        val encryptedPassword = BCrypt.hashpw(input.password, salt)
        val user = UserEntity(
            name = input.name,
            encryptedPassword = encryptedPassword,
        )
        return userRepository.save(user).toUser()
    }

    override suspend fun findByName(target: String): User? {
        require(target.isNotBlank()) { "Target name is blank." }
        return userRepository.findByName(target)?.toUser()
    }

    private fun UserEntity.toUser(): User {
        return User(name, encryptedPassword)
    }
}