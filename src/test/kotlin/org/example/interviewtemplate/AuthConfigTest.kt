package org.example.interviewtemplate

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.example.interviewtemplate.dto.LoggedUser
import org.example.interviewtemplate.dto.LoginUser
import org.example.interviewtemplate.dto.RegisterUser
import org.example.interviewtemplate.dto.User
import org.example.interviewtemplate.repositories.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.toEntity
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthConfigTest(
    @Autowired
    private val webClient: WebClient,
    @LocalServerPort
    private val port: Int,
    @Autowired
    private val userRepository: UserRepository
) {
    private val userApiUrl = defaultUrl(port)

    @AfterEach
    fun afterEach(): Unit = runBlocking {
        userRepository.deleteAll()
    }

    @Test
    fun testLogin(): Unit = runBlocking {
        val registerUser = RegisterUser(randomName(), randomPass())
        registerUser(registerUser)
        val response = webClient.post()
            .uri("$userApiUrl/users/login")
            .bodyValue(LoginUser(registerUser.name, registerUser.password))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity<LoggedUser>()
            .awaitSingle()
        assert(response.statusCode.value() == 200)
        assertNotNull(response.body)
    }

    @Test
    fun testAuthorize(): Unit = runBlocking {
        val registerUser = RegisterUser(randomName(), randomPass())
        registerUser(registerUser)
        val response = webClient.post()
            .uri("$userApiUrl/users/login")
            .bodyValue(LoginUser(registerUser.name, registerUser.password))
            .accept(MediaType.APPLICATION_JSON)
            .awaitRetrieveEntity<LoggedUser>()
        assert(response.statusCode.value() == 200)
        assertNotNull(response.body)
//        println(registerUser.name)
        val entity = webClient.get()
            .uri("$userApiUrl/users/${registerUser.name}")
            .accept(MediaType.APPLICATION_JSON)
            .awaitRetrieveEntity<User>()
        println(entity.body)
    }

    private suspend fun registerUser(user: RegisterUser): User {
        val response = webClient.post()
            .uri("$userApiUrl/users")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .retrieve()
            .toEntity<User>()
            .awaitSingle()
        assert(response.statusCode.value() == 201)
        val body = assertNotNull(response.body)
        return body
    }

    // Randomize name to avoid conflicts with concurrent calls in db,
    // since `name` column is unique.
    private fun randomName(): String = "name" + Random.nextInt(10000)
    private fun randomPass(): String = Random.nextInt(100).toString()

    @Test
    fun testFindByIdAuthReturns401() = runBlocking {
        val id = 7
        webClient.get()
            .uri("$userApiUrl/users/$id")
            .accept(MediaType.APPLICATION_JSON)
            .awaitExchange {
                assert(it.statusCode().value() == 401)
            }
    }
}