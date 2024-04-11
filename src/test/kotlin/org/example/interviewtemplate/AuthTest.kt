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
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.toEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthTest(
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
            .uri("$userApiUrl/auth/login")
            .bodyValue(LoginUser(registerUser.name, registerUser.password))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity<LoggedUser>()
            .awaitSingle()
        assert(response.statusCode.value() == 200)
        assertNotNull(response.body)
    }

    @Test
    fun testAuthorizeWithBearerToken() = runBlocking {
        val registerUser = RegisterUser(randomName(), randomPass())
        val user = registerUser(registerUser)
        val loggedUser = webClient.post()
            .uri("$userApiUrl/auth/login")
            .bodyValue(LoginUser(registerUser.name, registerUser.password))
            .accept(MediaType.APPLICATION_JSON)
            .awaitRetrieveEntity<LoggedUser>()
        webClient.get()
            .uri("$userApiUrl/users/${registerUser.name}")
            .accept(MediaType.APPLICATION_JSON)
            .headers { it.setBearerAuth(loggedUser.body!!.token) }
            .awaitExchange {
                assert(it.statusCode().value() == 200)
                val actual = it.awaitBody<User>()
                val expected = User(registerUser.name, user.id)
                assertEquals(expected, actual)
            }
    }

    @Test
    fun testMissingBearerToken() = runBlocking {
        val registerUser = RegisterUser(randomName(), randomPass())
        registerUser(registerUser)
        webClient.post()
            .uri("$userApiUrl/auth/login")
            .bodyValue(LoginUser(registerUser.name, registerUser.password))
            .accept(MediaType.APPLICATION_JSON)
            .awaitRetrieveEntity<LoggedUser>()
        webClient.get()
            .uri("$userApiUrl/users/${registerUser.name}")
            .accept(MediaType.APPLICATION_JSON)
            .awaitExchange {
                assert(it.statusCode().value() == 401)
            }
    }

    private suspend fun registerUser(user: RegisterUser): User {
        val response = webClient.post()
            .uri("$userApiUrl/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .awaitRetrieveEntity<User>()
        assert(response.statusCode.value() == 201)
        return requireNotNull(response.body)
    }
}