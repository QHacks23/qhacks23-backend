package com.example.user

import com.example.login.DLogin
import com.example.user.query.QDUser

class UserRepositoryImpl: UserRepository {
    override fun getByEbeansId(employeeId: Long): DUser? {
        return QDUser().id.eq(employeeId).findOne()
    }
    override fun getByFirebaseId(employeeId: String): DUser? {
        return QDUser().firebaseId.eq(employeeId).findOne()
    }

    override fun getByName(firstName: String, lastName: String): List<DUser> {
        return QDUser().firstName.eq(firstName).findList()
            .ifEmpty { return emptyList() }.filter { it.lastName == lastName }
    }

    override fun getByLogin(login: DLogin): DUser? {
        return QDUser().login.eq(login).findOne()
    }

    override fun getByEmail(email: String): DUser? {
        return QDUser().login.email.eq(email).findOne()
    }

}