package com.example.hellocowcow.domain.repositories

import com.example.hellocowcow.domain.models.RecoveryUnbondBatch

interface RecoveryHistoryRepository {
  suspend fun getPendingUnbondBatches(address: String): List<RecoveryUnbondBatch>
}
