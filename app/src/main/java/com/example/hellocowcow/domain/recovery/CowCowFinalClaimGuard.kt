package com.example.hellocowcow.domain.recovery

import com.example.hellocowcow.domain.models.RecoveryUnbondBatch

/**
 * Fail-closed validation used immediately before the final CowCow NFT claim.
 *
 * The screen only offers batches reconstructed from successful on-chain CowCow
 * unstake/claim history. Before signing, the history is rebuilt again and this
 * guard proves that the exact reviewed batch is still pending and has reached
 * the conservative delay observed in a successful historical final claim.
 */
object CowCowFinalClaimGuard {

  sealed interface Result {
    data class Ready(
      val batch: RecoveryUnbondBatch,
      val nonces: List<String>,
      val payload: String,
      val dataBase64: String
    ) : Result

    data class Blocked(val reason: String) : Result
  }

  fun validate(
    expectedBatch: RecoveryUnbondBatch,
    latestPendingBatches: List<RecoveryUnbondBatch>,
    nowEpochSeconds: Long
  ): Result {
    if (expectedBatch.cowNonces.isEmpty()) {
      return Result.Blocked("The selected CowCow unbond batch is empty")
    }

    val latestBatch = latestPendingBatches.firstOrNull {
      it.unstakeTxHash == expectedBatch.unstakeTxHash
    } ?: return Result.Blocked(
      "This CowCow unbond batch is no longer pending. Refresh Recovery before continuing."
    )

    if (latestBatch.cowNonces != expectedBatch.cowNonces) {
      return Result.Blocked(
        "The pending CowCow nonce state changed since this batch was reviewed. Refresh Recovery before continuing."
      )
    }

    if (nowEpochSeconds < latestBatch.claimableAtEpochSeconds) {
      val remaining = latestBatch.claimableAtEpochSeconds - nowEpochSeconds
      return Result.Blocked(
        "The conservative CowCow final-claim threshold has not been reached yet ($remaining seconds remaining)."
      )
    }

    val encoded = runCatching {
      CowCowFinalClaimPreviewCodec.encode(latestBatch.cowNonces)
    }.getOrElse { error ->
      return Result.Blocked(
        error.message ?: "Unable to encode the current CowCow final-claim payload"
      )
    }

    return Result.Ready(
      batch = latestBatch,
      nonces = encoded.nonces,
      payload = encoded.payload,
      dataBase64 = encoded.dataBase64
    )
  }
}
