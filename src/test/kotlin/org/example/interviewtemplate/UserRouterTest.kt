package org.example.interviewtemplate

import kotlinx.coroutines.runBlocking
import org.example.interviewtemplate.dto.RegisterUser
import org.example.interviewtemplate.dto.User
import org.example.interviewtemplate.repositories.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRouterTest(
    @Autowired
    private val webClient: WebClient,
    @Autowired
    private val userRepository: UserRepository,
    @LocalServerPort
    private val port: Int
) {
    private val userApiUrl = defaultUrl(port)

    private val registerUriApi = "$userApiUrl/auth/register"

    @AfterEach
    fun afterEach(): Unit = runBlocking {
        userRepository.deleteAll()
    }

    @Test
    fun testRegister(): Unit = runBlocking {
        val user = RegisterUser(randomName(), randomPass())
        val response = webClient.post()
            .uri(registerUriApi)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
        response.awaitExchange {
            assert(it.statusCode().value() == 201)
        }
    }

    @Test
    fun testRegisterDuplicates(): Unit = runBlocking {
        val user = RegisterUser(randomName(), randomPass())
        webClient.post()
            .uri(registerUriApi)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .awaitRetrieveEntity<User>()
        webClient.post()
            .uri(registerUriApi)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .awaitExchange {
                assert(it.statusCode().value() == 400)
            }
    }

    @Test
    fun testRegisterBadRequest() = runBlocking {
        val user = BadRegisterUser(randomName())
        val response = webClient.post()
            .uri(registerUriApi)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
        response.awaitExchange {
            assert(it.statusCode().is4xxClientError)
        }
    }

    data class BadRegisterUser(val name: String)

    @Test
    @WithMockUser
    fun testFindUserByName() = runBlocking {
        val user = RegisterUser(randomName(), randomPass())
        val response = webClient.post()
            .uri(registerUriApi)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .awaitRetrieveEntity<User>()
        assert(response.statusCode.value() == 201)
        webClient.get()
            .uri("$userApiUrl/users/${user.name}")
            .accept(MediaType.APPLICATION_JSON)
            .awaitExchange {
                assert(it.statusCode().value() == 200)
                val actual = it.awaitBody<User>()
                val expected = User(user.name, requireNotNull(response.body?.id))
                assertEquals(expected, actual)
            }
    }
}

fun defaultUrl(port: Int): String = "http://localhost:$port/api/"