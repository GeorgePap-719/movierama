package org.example.interviewtemplate

import kotlinx.coroutines.runBlocking
import org.example.interviewtemplate.dto.*
import org.example.interviewtemplate.repositories.MovieOpinionRepository
import org.example.interviewtemplate.repositories.MovieRepository
import org.example.interviewtemplate.repositories.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MovieRouterTest(
    @Autowired
    private val webClient: WebClient,
    @Autowired
    private val userRepository: UserRepository,
    @Autowired
    private val movieRepository: MovieRepository,
    @Autowired
    private val movieOpinionRepository: MovieOpinionRepository,
    @LocalServerPort
    private val port: Int
) {
    private val baseUrl = defaultUrl(port)

    private val registerUriApi = "$baseUrl/auth/register"

    @AfterEach
    fun afterEach(): Unit = runBlocking {
//        userRepository.deleteAll()
//        movieRepository.deleteAll()
//        movieOpinionRepository.deleteAll()
    }

    @Test
    @WithMockUser
    fun testRegisterMovie(): Unit = runBlocking {
        val registerUser = RegisterUser(randomName(), randomPass())
        val user = registerUser(registerUser)
        val newMovie = RegisterMovie(
            "movie" + randomName(),
            "cool one",
            user.id
        )
        val response = webClient.post()
            .uri("$baseUrl/movies")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(newMovie)
            .awaitRetrieveEntity<Movie>()
        assert(response.statusCode.value() == 201)
        val expected = Movie(
            title = newMovie.title,
            description = newMovie.description,
            userId = newMovie.userId,
            date = "",
            likes = 0,
            hates = 0
        )
        assertMoviesEquals(expected, response.body)
    }

    @Test
    @WithMockUser
    fun testFindMovieByTitle(): Unit = runBlocking {
        val registerUser = RegisterUser(randomName(), randomPass())
        val user = registerUser(registerUser)
        val newMovie = RegisterMovie(
            "movie" + randomName(),
            "cool one",
            user.id
        )
        val newMovieResponse = webClient.post()
            .uri("$baseUrl/movies")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(newMovie)
            .awaitRetrieveEntity<Movie>()
        assert(newMovieResponse.statusCode.value() == 201)
        assertNotNull(newMovieResponse.body)
        val response = webClient.get()
            .uri("$baseUrl/movies/${newMovie.title}")
            .accept(MediaType.APPLICATION_JSON)
            .awaitRetrieveEntity<Movie>()
        assert(response.statusCode.value() == 200)
        assertNotNull(response.body)
        assertEquals(newMovieResponse.body, response.body)
    }

    @Test
    @WithMockUser
    fun testNotFindMovieByTitle(): Unit = runBlocking {
        val registerUser = RegisterUser(randomName(), randomPass())
        val user = registerUser(registerUser)
        val newMovie = RegisterMovie(
            "movie" + randomName(),
            "cool one",
            user.id
        )
        webClient.get()
            .uri("$baseUrl/movies/${newMovie.title}")
            .accept(MediaType.APPLICATION_JSON)
            .awaitExchange {
                assert(it.statusCode().value() == 404)
                assert(it.awaitBodyOrNull<Movie>() == null)
            }
    }

    // Scenarios to check:
    // - [x] A user can post an opinion for a movie.
    // - [] A user cannot post opinion for a movie he submitted.
    //

    @Test
    fun testPostOpinion(): Unit = runBlocking {
        val user1 = prepareUser()
        val newMovie = RegisterMovie(
            "movie" + randomName(),
            "cool one",
            user1.id
        )
        webClient.post()
            .uri("$baseUrl/movies")
            .accept(MediaType.APPLICATION_JSON)
            .headers { it.setBearerAuth(user1.info.token) }
            .bodyValue(newMovie)
            .awaitRetrieveEntity<Movie>()
        val user2 = prepareUser()
        val movieOpinion = MovieOpinion(
            newMovie.title,
            Opinion.LIKE
        )
        webClient.post()
            .uri("$baseUrl/movies/opinion")
            .accept(MediaType.APPLICATION_JSON)
            .headers { it.setBearerAuth(user2.info.token) }
            .bodyValue(movieOpinion)
            .awaitExchange {
                println(it.statusCode().value())
                println(it.awaitBodyOrNull())
            }
    }

    // Skip asserting `date`, as it is generated by db.
    private fun assertMoviesEquals(expected: Movie, actual: Movie?) {
        assertNotNull(actual)
        assertEquals(expected.title, actual.title)
        assertEquals(expected.description, actual.description)
        assertEquals(expected.userId, actual.userId)
        assertEquals(expected.likes, actual.likes)
        assertEquals(expected.hates, actual.hates)
        assert(actual.date.isNotBlank())
    }

    private suspend fun prepareUser(): LoggedUserWithId {
        val registerUser = RegisterUser(randomName(), randomPass())
        val user = registerUser(registerUser)
        val response = webClient.post()
            .uri("$baseUrl/auth/login")
            .bodyValue(LoginUser(registerUser.name, registerUser.password))
            .accept(MediaType.APPLICATION_JSON)
            .awaitRetrieveEntity<LoggedUser>()
        assert(response.statusCode.value() == 200)
        val body = assertNotNull(response.body)
        return LoggedUserWithId(body, user.id)
    }

    private class LoggedUserWithId(val info: LoggedUser, val id: Int)

    private suspend fun registerUser(user: RegisterUser): User {
        val response = webClient.post()
            .uri(registerUriApi)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .awaitRetrieveEntity<User>()
        assert(response.statusCode.value() == 201)
        return requireNotNull(response.body)
    }
}