package com.example.hellocowcow.domain.recovery

import com.example.hellocowcow.domain.models.RecoverySnapshot
import java.math.BigDecimal

object RecoveryPlanCalculator {

  fun create(
    claimableRewards: BigDecimal,
    walletMooveBalance: BigDecimal,
    contractMooveBalance: BigDecimal
  ): RecoverySnapshot {
    require(claimableRewards >= BigDecimal.ZERO) { "Claimable rewards cannot be negative" }
    require(walletMooveBalance >= BigDecimal.ZERO) { "Wallet MOOVE balance cannot be negative" }
    require(contractMooveBalance >= BigDecimal.ZERO) { "Contract MOOVE balance cannot be negative" }

    return RecoverySnapshot(
      claimableRewards = claimableRewards,
      walletMooveBalance = walletMooveBalance,
      contractMooveBalance = contractMooveBalance,
      amountToAcquire = claimableRewards
        .subtract(walletMooveBalance)
        .max(BigDecimal.ZERO),
      // The legacy recovery procedure requires the caller to prefund their full
      // reward amount. The contract's global balance is informational only.
      recommendedTopUp = claimableRewards
    )
  }
}
