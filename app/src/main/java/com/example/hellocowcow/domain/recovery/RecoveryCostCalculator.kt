package com.example.hellocowcow.domain.recovery

import java.math.BigDecimal
import java.math.MathContext

object RecoveryCostCalculator {

  fun calculate(input: RecoveryCostEstimateInput): RecoveryCostEstimate {
    require(input.buyCostEgld >= BigDecimal.ZERO)
    require(input.expectedSellReturnEgld >= BigDecimal.ZERO)
    require(input.minimumSellReturnEgld >= BigDecimal.ZERO)
    require(input.estimatedNetworkFeesEgld >= BigDecimal.ZERO)
    require(input.minimumSellReturnEgld <= input.expectedSellReturnEgld) {
      "Minimum sell return cannot exceed expected sell return"
    }

    val expectedDexLoss = input.buyCostEgld
      .subtract(input.expectedSellReturnEgld)
      .max(BigDecimal.ZERO)

    val worstCaseDexLoss = input.buyCostEgld
      .subtract(input.minimumSellReturnEgld)
      .max(BigDecimal.ZERO)

    val expectedTotalLoss = expectedDexLoss.add(input.estimatedNetworkFeesEgld)
    val worstCaseTotalLoss = worstCaseDexLoss.add(input.estimatedNetworkFeesEgld)

    val expectedRecoveryRatio = if (input.buyCostEgld.signum() == 0) {
      BigDecimal.ONE
    } else {
      input.expectedSellReturnEgld.divide(input.buyCostEgld, MathContext.DECIMAL64)
    }

    return RecoveryCostEstimate(
      expectedDexLossEgld = expectedDexLoss,
      worstCaseDexLossEgld = worstCaseDexLoss,
      estimatedNetworkFeesEgld = input.estimatedNetworkFeesEgld,
      expectedTotalLossEgld = expectedTotalLoss,
      worstCaseTotalLossEgld = worstCaseTotalLoss,
      expectedRecoveryRatio = expectedRecoveryRatio
    )
  }
}
