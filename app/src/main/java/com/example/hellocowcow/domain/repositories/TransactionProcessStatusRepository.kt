package com.example.hellocowcow.domain.repositories

import com.example.hellocowcow.domain.models.TransactionProcessStatus

interface TransactionProcessStatusRepository {
  suspend fun getProcessStatus(txHash: String): TransactionProcessStatus
}
