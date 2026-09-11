package com.example.hellocowcow.data.recovery

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.retrofit.mvxApi.response.Transactions
import com.example.hellocowcow.domain.models.RecoveryUnbondBatch
import com.example.hellocowcow.domain.recovery.RecoveryEvidence

object RecoveryHistoryMatcher {

  fun pendingBatches(
    unstakeTransactions: List<Transactions>,
    finalClaimTransactions: List<Transactions>
  ): List<RecoveryUnbondBatch> {
    val claimRecords = finalClaimTransactions.map { transaction ->
      val timestamp = requireTimestamp(transaction, CowCowConfig.FINAL_CLAIM_FUNCTION)
      val nonces = requireNonceArguments(transaction, CowCowConfig.FINAL_CLAIM_FUNCTION)
      timestamp to nonces.toSet()
    }

    return unstakeTransactions.mapNotNull { transaction ->
      val timestamp = requireTimestamp(transaction, CowCowConfig.UNSTAKE_FUNCTION)
      val txHash = requireNotNull(transaction.txHash?.takeIf { it.isNotBlank() }) {
        "Successful CowCow unstake history entry is missing its transaction hash"
      }
      val unstakedNonces = requireNonceArguments(transaction, CowCowConfig.UNSTAKE_FUNCTION)

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
        claimableAtEpochSeconds = timestamp +
            RecoveryEvidence.OBSERVED_SUCCESSFUL_FINAL_CLAIM_DELAY_SECONDS
      )
    }.sortedBy { it.unstakedAtEpochSeconds }
  }

  private fun requireTimestamp(transaction: Transactions, function: String): Long {
    return requireNotNull(transaction.timestamp?.toLong()) {
      "Successful CowCow $function history entry is missing its timestamp"
    }
  }

  private fun requireNonceArguments(
    transaction: Transactions,
    function: String
  ): List<String> {
    val nonces = CowCowCallDataParser.parseNonceArguments(
      data = transaction.data,
      function = function
    )
    require(nonces.isNotEmpty()) {
      val hash = transaction.txHash?.takeIf { it.isNotBlank() } ?: "unknown hash"
      "Unable to decode CowCow $function nonce arguments for $hash"
    }
    return nonces
  }
}
