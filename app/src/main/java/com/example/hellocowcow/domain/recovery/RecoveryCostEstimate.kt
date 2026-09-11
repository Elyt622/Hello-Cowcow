package com.example.hellocowcow.domain.recovery

import java.math.BigDecimal

data class RecoveryCostEstimateInput(
  val buyCostEgld: BigDecimal,
  val expectedSellReturnEgld: BigDecimal,
  val minimumSellReturnEgld: BigDecimal,
  val estimatedNetworkFeesEgld: BigDecimal
)

data class RecoveryCostEstimate(
  val expectedDexLossEgld: BigDecimal,
  val worstCaseDexLossEgld: BigDecimal,
  val estimatedNetworkFeesEgld: BigDecimal,
  val expectedTotalLossEgld: BigDecimal,
  val worstCaseTotalLossEgld: BigDecimal,
  val expectedRecoveryRatio: BigDecimal
)
