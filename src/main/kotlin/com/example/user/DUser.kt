package com.example.user

import com.example.domain.AbstractIdEntity
import com.example.login.DLogin
import com.example.nft.DCredit
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.OneToOne

@Entity
class DUser(
    val firebaseId: String,
    val firstName: String,
    val lastName: String,
    @OneToOne
    val login: DLogin,
    val accountId: String,
    val publicKey: String,
    @OneToMany
    val credits: MutableList<DCredit>
): AbstractIdEntity() {
}