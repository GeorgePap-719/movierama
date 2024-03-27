package org.example.interviewtemplate

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

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

    @AfterEach
    fun afterEach(): Unit = runBlocking {
        //TODO: userRepository.deleteAll()
    }

    @Test
    fun testRegister() = runBlocking {
        val user = RegisterUser("george", "pap", "6923")
        val response = webClient.post()
            .uri("$userApiUrl/register")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
        response.awaitExchange {
            assert(it.statusCode().value() == 200)
            val newUser = it.awaitBody<User>()
            println("response:$newUser")
        }
    }

    @Test
    fun testBadRequest() = runBlocking {
        val user = BadRegisterUser("george")
        val response = webClient.post()
            .uri("$userApiUrl/register")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
        response.awaitExchange {
            assert(it.statusCode().is4xxClientError)
            println(it.awaitBody<ErrorMessage>())
        }
    }

    data class BadRegisterUser(val name: String)
}

fun defaultUrl(port: Int): String = "http://localhost:$port/api/"