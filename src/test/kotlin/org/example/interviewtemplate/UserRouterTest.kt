package org.example.interviewtemplate

import kotlinx.coroutines.reactor.awaitSingle
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
import org.springframework.web.reactive.function.client.toEntity
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        userRepository.deleteAll()
    }

    @Test
    fun testRegister(): Unit = runBlocking {
        val user = RegisterUser("georgeAA", "pap")
        val response = webClient.post()
            .uri("$userApiUrl/users")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
        response.awaitExchange {
            assert(it.statusCode().value() == 201)
            it.awaitBody<User>()
        }
    }

    @Test
    fun testRegisterDuplicates(): Unit = runBlocking {
        val user = RegisterUser("george", "pap")
        webClient.post()
            .uri("$userApiUrl/users")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .retrieve()
            .awaitBody<User>()
        webClient.post()
            .uri("$userApiUrl/users")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .awaitExchange {
                assert(it.statusCode().value() == 400)
                println(it.awaitBody<String>())
            }
    }

    @Test
    fun testRegisterBadRequest() = runBlocking {
        val user = BadRegisterUser("george")
        val response = webClient.post()
            .uri("$userApiUrl/users")
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
        // Randomize name to avoid conflicts with concurrent calls in db,
        // since `name` column is unique.
        val name = "george" + Random.nextInt(100)
        val user = RegisterUser(name, Random.nextInt(100).toString())
        val response = webClient.post()
            .uri("$userApiUrl/users")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .retrieve()
            .toEntity<User>()
            .awaitSingle()
        assert(response.statusCode.value() == 201)
        val registeredUser = response.body
        assertNotNull(registeredUser)
        webClient.get()
            .uri("$userApiUrl/users/${registeredUser.name}")
            .accept(MediaType.APPLICATION_JSON)
            .awaitExchange {
                assert(it.statusCode().value() == 200)
                val actualUser = it.awaitBody<User>()
                assertEquals(registeredUser, actualUser)
            }
    }
}

fun defaultUrl(port: Int): String = "http://localhost:$port/api/"