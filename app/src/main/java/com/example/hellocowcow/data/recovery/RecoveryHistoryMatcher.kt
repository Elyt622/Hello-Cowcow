package com.example.hellocowcow.data.recovery

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.retrofit.mvxApi.response.Transactions
import com.example.hellocowcow.domain.models.RecoveryUnbondBatch

object RecoveryHistoryMatcher {

  fun pendingBatches(
    unstakeTransactions: List<Transactions>,
    finalClaimTransactions: List<Transactions>
  ): List<RecoveryUnbondBatch> {
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
}
