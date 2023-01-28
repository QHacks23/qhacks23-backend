package com.example.login

interface LoginRepository {
    fun getByEmail(email: String): DLogin?
}