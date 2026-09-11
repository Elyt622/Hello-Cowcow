package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.network.api.MvxApi
import com.example.hellocowcow.data.recovery.CowCowCallDataParser
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
      function = CowCowConfig.UNSTAKE_FUNCTION
    ).filter(::isSuccessfulCowCowCall)

    val finalClaimTransactions = mvxApi.getTransactionsByFunction(
      sender = address,
      receiver = CowCowConfig.REWARDS_CONTRACT,
      function = CowCowConfig.FINAL_CLAIM_FUNCTION
    ).filter(::isSuccessfulCowCowCall)

    val claimRecords = finalClaimTransactions.mapNotNull { transaction ->
      val timestamp = transaction.timestamp?.toLong() ?: return@mapNotNull null
      val nonces = CowCowCallDataParser.parseNonceArguments(
        data = transaction.data,
        function = CowCowConfig.FINAL_CLAIM_FUNCTION
      )
      if (nonces.isEmpty()) null else timestamp to nonces.toSet()
    }

    return unstakeTransactions.mapNotNull { transaction ->
      val timestamp = transaction.timestamp?.toLong() ?: return@mapNotNull null
      val txHash = transaction.txHash?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
      val unstakedNonces = CowCowCallDataParser.parseNonceArguments(
        data = transaction.data,
        function = CowCowConfig.UNSTAKE_FUNCTION
      )
      if (unstakedNonces.isEmpty()) return@mapNotNull null

      val claimedAfterUnstake = claimRecords
        .asSequence()
        .filter { (claimTimestamp, _) -> claimTimestamp >= timestamp }
        .flatMap { (_, nonces) -> nonces.asSequence() }
        .toSet()

      val pendingNonces = unstakedNonces.filterNot(claimedAfterUnstake::contains)
      if (pendingNonces.isEmpty()) return@mapNotNull null

      RecoveryUnbondBatch(
        unstakeTxHash = txHash,
        cowNonces = pendingNonces,
        unstakedAtEpochSeconds = timestamp,
        claimableAtEpochSeconds = timestamp + CowCowConfig.UNBONDING_PERIOD_SECONDS
      )
    }.sortedBy { it.unstakedAtEpochSeconds }
  }

  private fun isSuccessfulCowCowCall(transaction: Transactions): Boolean {
    return transaction.receiver == CowCowConfig.REWARDS_CONTRACT &&
        transaction.status.equals("success", ignoreCase = true)
  }
}
