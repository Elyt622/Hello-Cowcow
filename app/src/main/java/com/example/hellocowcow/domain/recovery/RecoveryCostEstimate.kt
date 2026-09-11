package com.example.hellocowcow.domain.recovery

import java.math.BigDecimal

data class RecoveryCostEstimateInput(
  val buyCostEgld: BigDecimal,
  val expectedSellReturnEgld: BigDecimal,
  val minimumSellReturnEgld: BigDecimal,
  val estimatedNetworkFeesEgld: BigDecimal
)

data class RecoveryCostEstimate(
  val expectedLossEgld: BigDecimal,
  val worstCaseLossEgld: BigDecimal,
  val expectedRecoveryRatio: BigDecimal
)
