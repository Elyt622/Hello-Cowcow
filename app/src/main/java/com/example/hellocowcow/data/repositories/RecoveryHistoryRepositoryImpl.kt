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

    val unstakeTransactions = loadCompleteHistory(
      address = address,
      function = CowCowConfig.UNSTAKE_FUNCTION
    )
    val finalClaimTransactions = loadCompleteHistory(
      address = address,
      function = CowCowConfig.FINAL_CLAIM_FUNCTION
    )

    return RecoveryHistoryMatcher.pendingBatches(
      unstakeTransactions = unstakeTransactions,
      finalClaimTransactions = finalClaimTransactions
    )
  }

  private suspend fun loadCompleteHistory(
    address: String,
    function: String
  ): List<Transactions> {
    val transactions = mvxApi.getTransactionsByFunction(
      sender = address,
      receiver = CowCowConfig.REWARDS_CONTRACT,
      function = function,
      size = HISTORY_PAGE_SIZE
    )

    check(transactions.size < HISTORY_PAGE_SIZE) {
      "CowCow $function history reached the $HISTORY_PAGE_SIZE-transaction read limit; Recovery cannot prove that the reconstructed exit state is complete"
    }

    return transactions.filter(::isSuccessfulCowCowCall)
  }

  private fun isSuccessfulCowCowCall(transaction: Transactions): Boolean {
    return transaction.receiver == CowCowConfig.REWARDS_CONTRACT &&
        transaction.status.equals("success", ignoreCase = true)
  }

  private companion object {
    // MultiversX API list endpoints support much larger result sets, but keeping
    // this request bounded protects the screen from unexpectedly heavy history
    // reads. If the bound is ever hit we fail closed instead of treating a
    // potentially truncated history as authoritative.
    const val HISTORY_PAGE_SIZE = 1000
  }
}
