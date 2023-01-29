package com.example.nft

import com.example.domain.AbstractIdEntity
import com.example.user.DUser
import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class DCredit(
    val tokenId: String,
    @ManyToOne
    var owner: DUser,
    var available: Boolean = true,
    var value: Long
): AbstractIdEntity() {
}