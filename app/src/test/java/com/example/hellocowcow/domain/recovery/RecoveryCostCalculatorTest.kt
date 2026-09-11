package com.example.hellocowcow.domain.recovery

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RecoveryCostCalculatorTest {

  @Test
  fun `calculates expected and worst case loss from round trip quotes`() {
    val estimate = RecoveryCostCalculator.calculate(
      RecoveryCostEstimateInput(
        buyCostEgld = BigDecimal("0.1000"),
        expectedSellReturnEgld = BigDecimal("0.0988"),
        minimumSellReturnEgld = BigDecimal("0.0975"),
        estimatedNetworkFeesEgld = BigDecimal("0.0006")
      )
    )

    assertEquals(BigDecimal("0.0018"), estimate.expectedLossEgld)
    assertEquals(BigDecimal("0.0031"), estimate.worstCaseLossEgld)
  }

  @Test
  fun `does not report negative loss when sell quote exceeds buy cost`() {
    val estimate = RecoveryCostCalculator.calculate(
      RecoveryCostEstimateInput(
        buyCostEgld = BigDecimal("0.1000"),
        expectedSellReturnEgld = BigDecimal("0.1010"),
        minimumSellReturnEgld = BigDecimal("0.1000"),
        estimatedNetworkFeesEgld = BigDecimal("0.0005")
      )
    )

    assertEquals(BigDecimal.ZERO, estimate.expectedLossEgld)
  }

  @Test
  fun `rejects minimum return greater than expected return`() {
    assertThrows(IllegalArgumentException::class.java) {
      RecoveryCostCalculator.calculate(
        RecoveryCostEstimateInput(
          buyCostEgld = BigDecimal.ONE,
          expectedSellReturnEgld = BigDecimal("0.9"),
          minimumSellReturnEgld = BigDecimal("0.95"),
          estimatedNetworkFeesEgld = BigDecimal.ZERO
        )
      )
    }
  }

  @Test
  fun `zero buy cost uses full recovery ratio`() {
    val estimate = RecoveryCostCalculator.calculate(
      RecoveryCostEstimateInput(
        buyCostEgld = BigDecimal.ZERO,
        expectedSellReturnEgld = BigDecimal.ZERO,
        minimumSellReturnEgld = BigDecimal.ZERO,
        estimatedNetworkFeesEgld = BigDecimal("0.0002")
      )
    )

    assertEquals(BigDecimal.ONE, estimate.expectedRecoveryRatio)
    assertEquals(BigDecimal("0.0002"), estimate.expectedLossEgld)
  }
}
