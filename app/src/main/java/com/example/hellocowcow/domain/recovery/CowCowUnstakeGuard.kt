package com.example.hellocowcow.domain.recovery

import com.example.hellocowcow.domain.models.RecoverySnapshot
import java.math.BigDecimal

/**
 * Pure, non-executing validation used immediately before a CowCow unstake flow.
 *
 * This deliberately does not build, sign, or broadcast a transaction. It only
 * verifies that the current on-chain state is still compatible with the state
 * the user reviewed on screen.
 */
object CowCowUnstakeGuard {

  sealed interface Result {
    data class Ready(
      val nonces: List<String>,
      val payload: String,
      val dataBase64: String,
      val rewardsMoove: BigDecimal,
      val contractMooveBalance: BigDecimal
    ) : Result

    data class Blocked(val reason: String) : Result
  }

  fun validate(
    expectedNonces: List<String>,
    latestSnapshot: RecoverySnapshot,
    latestNonces: List<String>
  ): Result {
    if (expectedNonces.isEmpty()) {
      return Result.Blocked("No staked CowCows were selected for exit")
    }

    if (latestNonces.isEmpty()) {
      return Result.Blocked("No staked CowCows are currently reported on-chain")
    }

    if (latestNonces != expectedNonces) {
      return Result.Blocked(
        "The CowCow staking state changed since this screen was loaded. Refresh before continuing."
      )
    }

    if (latestSnapshot.claimLiquidityGap > BigDecimal.ZERO) {
      return Result.Blocked(
        "CowCow staking is still missing ${latestSnapshot.claimLiquidityGap.stripTrailingZeros().toPlainString()} MOOVE. Restore contract liquidity before unstaking."
      )
    }

    val encoded = runCatching {
      CowCowUnstakePreviewCodec.encode(latestNonces)
    }.getOrElse { error ->
      return Result.Blocked(
        error.message ?: "Unable to encode the current CowCow unstake payload"
      )
    }

    return Result.Ready(
      nonces = encoded.nonces,
      payload = encoded.payload,
      dataBase64 = encoded.dataBase64,
      rewardsMoove = latestSnapshot.claimableRewards,
      contractMooveBalance = latestSnapshot.contractMooveBalance
    )
  }
}
