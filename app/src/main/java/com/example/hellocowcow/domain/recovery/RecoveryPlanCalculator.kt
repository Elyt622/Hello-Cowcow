package com.example.hellocowcow.domain.recovery

import com.example.hellocowcow.domain.models.RecoverySnapshot
import java.math.BigDecimal

object RecoveryPlanCalculator {

  fun create(
    claimableRewards: BigDecimal,
    walletMooveBalance: BigDecimal,
    contractMooveBalance: BigDecimal,
    stakedCowNonces: List<String> = emptyList()
  ): RecoverySnapshot {
    require(claimableRewards >= BigDecimal.ZERO) { "Claimable rewards cannot be negative" }
    require(walletMooveBalance >= BigDecimal.ZERO) { "Wallet MOOVE balance cannot be negative" }
    require(contractMooveBalance >= BigDecimal.ZERO) { "Contract MOOVE balance cannot be negative" }

    val normalizedNonces = stakedCowNonces.map { nonce ->
      nonce.lowercase().also {
        require(it.matches(Regex("[0-9a-f]{4}"))) {
          "CowCow nonce must be a four-character hexadecimal value"
        }
        require(it != "0000") { "CowCow nonce cannot be zero" }
      }
    }.distinct()

    // Claim-first recovery only needs enough temporary liquidity for the contract
    // to honor the pending reward payment. Existing contract MOOVE contributes to
    // that payout, and MOOVE already in the wallet can be reused as temporary
    // liquidity before buying anything on xExchange.
    val claimLiquidityGap = claimableRewards
      .subtract(contractMooveBalance)
      .max(BigDecimal.ZERO)

    val amountToAcquire = claimLiquidityGap
      .subtract(walletMooveBalance)
      .max(BigDecimal.ZERO)

    return RecoverySnapshot(
      claimableRewards = claimableRewards,
      walletMooveBalance = walletMooveBalance,
      contractMooveBalance = contractMooveBalance,
      claimLiquidityGap = claimLiquidityGap,
      amountToAcquire = amountToAcquire,
      recommendedTopUp = claimLiquidityGap,
      stakedCowNonces = normalizedNonces
    )
  }
}
