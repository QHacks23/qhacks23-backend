package com.example.user

import com.example.login.DLogin

interface UserRepository {
    fun getByEbeansId(employeeId: Long): DUser?
    fun getByFirebaseId(employeeId: String): DUser?
    fun getByName(firstName: String, lastName: String): List<DUser>
    fun getByLogin(login: DLogin): DUser?
    fun getByEmail(email: String): DUser?
}