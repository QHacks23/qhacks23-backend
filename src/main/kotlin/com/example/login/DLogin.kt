package com.example.login

import com.example.domain.AbstractIdEntity
import com.example.user.DUser
import javax.persistence.Entity
import javax.persistence.OneToOne

@Entity
class DLogin(
    @OneToOne
    val email: String,
    val password: String,
    @OneToOne
    var user: DUser? = null,
    var organization: String? = null,
    )
: AbstractIdEntity() {
}