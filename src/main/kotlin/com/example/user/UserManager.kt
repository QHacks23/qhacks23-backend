package com.example.user

interface UserManager {
    sealed class Result {
        data class Success(val user: DUser, val mnemonicString: String?) : Result()
        sealed class Error(val message: String) : Result() {
            class NotFound(message: String) : Error(message)
            class Invalid(message: String) : Error(message)
        }
    }
    fun createUser(
        id: String,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): Result

    fun loginUser(email: String, password: String): Result
}