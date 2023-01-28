package com.example.domain

import io.ebean.Model
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AbstractIdEntity: Model() {
    @Id
    var id: Long = 0
}