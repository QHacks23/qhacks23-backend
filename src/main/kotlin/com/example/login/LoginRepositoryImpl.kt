package com.example.login

import com.example.login.query.QDLogin

class LoginRepositoryImpl: LoginRepository {
    override fun getByEmail(email: String): DLogin? {
        return QDLogin().email.eq(email).findOne()
    }
}