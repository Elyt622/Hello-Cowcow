package com.example.hellocowcow.domain.repositories

import com.example.hellocowcow.domain.models.DomainTransaction
import com.example.hellocowcow.domain.models.MvxTransaction

interface TransactionRepository {
  suspend fun sendTransaction(tx: MvxTransaction): DomainTransaction
}
