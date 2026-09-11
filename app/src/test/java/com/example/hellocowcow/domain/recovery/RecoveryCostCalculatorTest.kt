package com.example.hellocowcow.domain.recovery

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RecoveryCostCalculatorTest {

  @Test
  fun `separates DEX friction network fees and total loss`() {
    val estimate = RecoveryCostCalculator.calculate(
      RecoveryCostEstimateInput(
        buyCostEgld = BigDecimal("0.1000"),
        expectedSellReturnEgld = BigDecimal("0.0988"),
        minimumSellReturnEgld = BigDecimal("0.0975"),
        estimatedNetworkFeesEgld = BigDecimal("0.0006"),
        maximumNetworkFeesEgld = BigDecimal("0.0010")
      )
    )

    assertEquals(BigDecimal("0.0012"), estimate.expectedDexLossEgld)
    assertEquals(BigDecimal("0.0025"), estimate.worstCaseDexLossEgld)
    assertEquals(BigDecimal("0.0006"), estimate.estimatedNetworkFeesEgld)
    assertEquals(BigDecimal("0.0010"), estimate.maximumNetworkFeesEgld)
    assertEquals(BigDecimal("0.0018"), estimate.expectedTotalLossEgld)
    assertEquals(BigDecimal("0.0035"), estimate.worstCaseTotalLossEgld)
  }

  @Test
  fun `does not report negative DEX loss when sell quote exceeds buy cost`() {
    val estimate = RecoveryCostCalculator.calculate(
      RecoveryCostEstimateInput(
        buyCostEgld = BigDecimal("0.1000"),
        expectedSellReturnEgld = BigDecimal("0.1010"),
        minimumSellReturnEgld = BigDecimal("0.1000"),
        estimatedNetworkFeesEgld = BigDecimal("0.0005")
      )
    )

    assertEquals(BigDecimal.ZERO, estimate.expectedDexLossEgld)
    assertEquals(BigDecimal("0.0005"), estimate.expectedTotalLossEgld)
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
  fun `rejects maximum network fee below simulated fee`() {
    assertThrows(IllegalArgumentException::class.java) {
      RecoveryCostCalculator.calculate(
        RecoveryCostEstimateInput(
          buyCostEgld = BigDecimal.ZERO,
          expectedSellReturnEgld = BigDecimal.ZERO,
          minimumSellReturnEgld = BigDecimal.ZERO,
          estimatedNetworkFeesEgld = BigDecimal("0.001"),
          maximumNetworkFeesEgld = BigDecimal("0.0009")
        )
      )
    }
  }

  @Test
  fun `zero buy cost leaves only network fees`() {
    val estimate = RecoveryCostCalculator.calculate(
      RecoveryCostEstimateInput(
        buyCostEgld = BigDecimal.ZERO,
        expectedSellReturnEgld = BigDecimal.ZERO,
        minimumSellReturnEgld = BigDecimal.ZERO,
        estimatedNetworkFeesEgld = BigDecimal("0.0002"),
        maximumNetworkFeesEgld = BigDecimal("0.0004")
      )
    )

    assertEquals(BigDecimal.ONE, estimate.expectedRecoveryRatio)
    assertEquals(BigDecimal.ZERO, estimate.expectedDexLossEgld)
    assertEquals(BigDecimal("0.0002"), estimate.expectedTotalLossEgld)
    assertEquals(BigDecimal("0.0004"), estimate.worstCaseTotalLossEgld)
  }
}
