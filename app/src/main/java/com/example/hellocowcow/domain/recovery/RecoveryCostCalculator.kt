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

    val expectedLoss = input.buyCostEgld
      .subtract(input.expectedSellReturnEgld)
      .add(input.estimatedNetworkFeesEgld)
      .max(BigDecimal.ZERO)

    val worstCaseLoss = input.buyCostEgld
      .subtract(input.minimumSellReturnEgld)
      .add(input.estimatedNetworkFeesEgld)
      .max(BigDecimal.ZERO)

    val expectedRecoveryRatio = if (input.buyCostEgld.signum() == 0) {
      BigDecimal.ONE
    } else {
      input.expectedSellReturnEgld.divide(input.buyCostEgld, MathContext.DECIMAL64)
    }

    return RecoveryCostEstimate(
      expectedLossEgld = expectedLoss,
      worstCaseLossEgld = worstCaseLoss,
      expectedRecoveryRatio = expectedRecoveryRatio
    )
  }
}
