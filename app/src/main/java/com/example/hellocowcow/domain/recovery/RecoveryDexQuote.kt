package com.example.hellocowcow.domain.recovery

import java.math.BigDecimal

data class RecoveryDexQuote(
  val mooveAmount: BigDecimal,
  val buyCostEgld: BigDecimal,
  val expectedSellReturnEgld: BigDecimal,
  val minimumSellReturnEgld: BigDecimal,
  val tolerancePercentage: BigDecimal,
  val buyMaxPriceDeviationPercent: BigDecimal? = null,
  val sellMaxPriceDeviationPercent: BigDecimal? = null
)
