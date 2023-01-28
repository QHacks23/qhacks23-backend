package com.example.user

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable


@Serializable
data class HttpGetUser(
    val id: String,
    val firstName: String,
    val lastName: String,
    val mnemonic: String
)
@Serializable
data class HttpPostSignup(
    val userId: String,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class HttpGetUsers(val employees: List<HttpGetUser>)
@Serializable
data class HttpPostUserLogin(val email: String, val password: String)

fun Route.userRoutes(userManager: UserManager, userRepository: UserRepository) {
    route("/user") {
        get {
            val userId = call.request.queryParameters["id"]
            val firstName = call.request.queryParameters["firstName"]
            val lastName = call.request.queryParameters["lastName"]

            val users = if (!userId.isNullOrEmpty()) {
                val user = userRepository.getByFirebaseId(userId) ?: return@get call.respondText(
                    "No user found with id $userId",
                    status = HttpStatusCode.OK
                )
                listOf(user)
            } else if (!firstName.isNullOrEmpty() && !lastName.isNullOrEmpty()) {
                userRepository.getByName(firstName, lastName)
            } else {
                return@get call.respondText(
                    "Bad query params",
                    status = HttpStatusCode.BadRequest
                )
            }
            call.respond(
                HttpGetUsers(
                    users.map { user ->
                        HttpGetUser(
                            id = user.firebaseId,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            mnemonic = ""
                        )
                    }
                )
            )
        }
    }
    route("/register") {
        post {
            val signup = call.receive<HttpPostSignup>()
            when (val result = userManager.createUser(
                signup.userId,
                signup.email,
                signup.password,
                signup.firstName,
                signup.lastName
            )) {
                is UserManager.Result.Error -> {
                    return@post call.respondText(
                        "Invalid request",
                        status = HttpStatusCode.BadRequest
                    )
                }

                is UserManager.Result.Success -> {
                    return@post call.respond(
                        HttpGetUser(
                            id = result.user.firebaseId,
                            firstName = result.user.firstName,
                            lastName = result.user.lastName,
                            mnemonic = result.mnemonicString ?: throw Exception("could not fetch mnemonic")

                        )
                    )
                }
            }
        }
    }
}