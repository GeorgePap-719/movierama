package org.example.interviewtemplate

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthConfigTest(
    @Autowired
    private val webClient: WebClient,
    @LocalServerPort
    private val port: Int
) {
    private val userApiUrl = defaultUrl(port)

    @Test
    fun testFindByIdAuth(): Unit = runBlocking {
        val id = 7
        webClient.get()
            .uri("$userApiUrl/users/$id")
            .headers { it.setBasicAuth("george", "123") }
            .accept(MediaType.APPLICATION_JSON)
            .awaitExchange {
                assert(it.statusCode().value() == 200)
            }
    }

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