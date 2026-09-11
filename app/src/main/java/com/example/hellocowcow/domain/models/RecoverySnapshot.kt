package com.example.hellocowcow.domain.models

import java.math.BigDecimal

data class RecoverySnapshot(
  val claimableRewards: BigDecimal,
  val walletMooveBalance: BigDecimal,
  val contractMooveBalance: BigDecimal,
  val claimLiquidityGap: BigDecimal,
  val amountToAcquire: BigDecimal,
  val recommendedTopUp: BigDecimal,
  val stakedCowNonces: List<String>
)
