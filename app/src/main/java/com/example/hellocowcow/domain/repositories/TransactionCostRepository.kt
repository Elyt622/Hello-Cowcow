package com.example.hellocowcow.domain.repositories

import com.example.hellocowcow.domain.models.MvxTransaction
import com.example.hellocowcow.domain.recovery.TransactionFeeEstimate

interface TransactionCostRepository {
  suspend fun estimateFee(transaction: MvxTransaction): TransactionFeeEstimate
}
