package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.data.network.api.MvxApi
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.repositories.AccountRepository
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
  private val mvxApi: MvxApi
) : AccountRepository {

  override suspend fun getAccount(
    address: String
  ): DomainAccount = mvxApi.getAccount(address).toDomain()

}
