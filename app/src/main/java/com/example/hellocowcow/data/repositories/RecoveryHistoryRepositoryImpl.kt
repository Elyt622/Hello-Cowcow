package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.network.api.MvxApi
import com.example.hellocowcow.data.recovery.RecoveryHistoryMatcher
import com.example.hellocowcow.data.retrofit.mvxApi.response.Transactions
import com.example.hellocowcow.domain.models.RecoveryUnbondBatch
import com.example.hellocowcow.domain.repositories.RecoveryHistoryRepository
import javax.inject.Inject

class RecoveryHistoryRepositoryImpl @Inject constructor(
  private val mvxApi: MvxApi
) : RecoveryHistoryRepository {

  override suspend fun getPendingUnbondBatches(address: String): List<RecoveryUnbondBatch> {
    require(address.isNotBlank()) { "Wallet address is required to rebuild Recovery history" }

    val unstakeTransactions = mvxApi.getTransactionsByFunction(
      sender = address,
      receiver = CowCowConfig.REWARDS_CONTRACT,
      function = CowCowConfig.UNSTAKE_FUNCTION,
      size = HISTORY_PAGE_SIZE
    ).filter(::isSuccessfulCowCowCall)

    val finalClaimTransactions = mvxApi.getTransactionsByFunction(
      sender = address,
      receiver = CowCowConfig.REWARDS_CONTRACT,
      function = CowCowConfig.FINAL_CLAIM_FUNCTION,
      size = HISTORY_PAGE_SIZE
    ).filter(::isSuccessfulCowCowCall)

    return RecoveryHistoryMatcher.pendingBatches(
      unstakeTransactions = unstakeTransactions,
      finalClaimTransactions = finalClaimTransactions
    )
  }

  private fun isSuccessfulCowCowCall(transaction: Transactions): Boolean {
    return transaction.receiver == CowCowConfig.REWARDS_CONTRACT &&
        transaction.status.equals("success", ignoreCase = true)
  }

  private companion object {
    // MultiversX API list endpoints allow large page sizes up to the endpoint
    // complexity ceiling. One thousand is enough to avoid silently truncating a
    // normal wallet's CowCow history while keeping this to one request per method.
    const val HISTORY_PAGE_SIZE = 1000
  }
}
