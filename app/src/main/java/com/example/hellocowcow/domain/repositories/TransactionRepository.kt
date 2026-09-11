package com.example.hellocowcow.domain.repositories

import com.example.hellocowcow.data.retrofit.mvxApi.request.Transaction
import com.example.hellocowcow.domain.models.DomainTransaction

interface TransactionRepository {

  suspend fun sendTransaction(
    tx: Transaction
  ): DomainTransaction
}
