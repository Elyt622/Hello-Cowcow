package com.example.hellocowcow.domain.repositories

import com.example.hellocowcow.domain.models.DomainAccount

interface AccountRepository {

  suspend fun getAccount(
    address: String
  ): DomainAccount

}
