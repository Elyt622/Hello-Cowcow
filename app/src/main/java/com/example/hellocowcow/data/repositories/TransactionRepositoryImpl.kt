package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.data.network.api.MvxApi
import com.example.hellocowcow.data.transaction.toRequest
import com.example.hellocowcow.domain.models.DomainTransaction
import com.example.hellocowcow.domain.models.MvxTransaction
import com.example.hellocowcow.domain.repositories.TransactionRepository
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
  private val mvxApi: MvxApi
) : TransactionRepository {

  override suspend fun sendTransaction(
    tx: MvxTransaction
  ): DomainTransaction = mvxApi.sendTransaction(tx.toRequest()).toDomain()
}
