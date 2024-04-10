package org.example.interviewtemplate

import kotlinx.coroutines.runBlocking
import org.example.interviewtemplate.dto.Movie
import org.example.interviewtemplate.dto.RegisterMovie
import org.example.interviewtemplate.dto.RegisterUser
import org.example.interviewtemplate.dto.User
import org.example.interviewtemplate.repositories.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.web.reactive.function.client.WebClient
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MovieRouterTest(
    @Autowired
    private val webClient: WebClient,
    @Autowired
    private val userRepository: UserRepository,
    @LocalServerPort
    private val port: Int
) {
    private val baseUrl = defaultUrl(port)

    private val registerUriApi = "$baseUrl/auth/register"

    @AfterEach
    fun afterEach(): Unit = runBlocking {
        //userRepository.deleteAll()
    }

    @Test
    @WithMockUser
    fun testRegisterMovie(): Unit = runBlocking {
        val registerUser = RegisterUser(randomName(), randomPass())
        val user = registerUser(registerUser)
        val newMovie = RegisterMovie(
            "random",
            "cool one",
            user.id
        )
        val response = webClient.post()
            .uri("$baseUrl/movies")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(newMovie)
            .awaitRetrieveEntity<Movie>()
        println(response.statusCode)
        println(response.body)
    }

    private suspend fun registerUser(user: RegisterUser): User {
        val response = webClient.post()
            .uri("$baseUrl/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .awaitRetrieveEntity<User>()
        assert(response.statusCode.value() == 201)
        return requireNotNull(response.body)
    }
}