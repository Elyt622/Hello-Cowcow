package com.example.hellocowcow.domain.recovery

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class RecoveryPlanCalculatorTest {

  @Test
  fun `missing amount is rewards minus wallet balance`() {
    val snapshot = RecoveryPlanCalculator.create(
      claimableRewards = BigDecimal("1000"),
      walletMooveBalance = BigDecimal("250"),
      contractMooveBalance = BigDecimal("50000")
    )

    assertEquals(0, snapshot.amountToAcquire.compareTo(BigDecimal("750")))
    assertEquals(0, snapshot.recommendedTopUp.compareTo(BigDecimal("1000")))
  }

  @Test
  fun `nothing must be acquired when wallet already covers rewards`() {
    val snapshot = RecoveryPlanCalculator.create(
      claimableRewards = BigDecimal("1000"),
      walletMooveBalance = BigDecimal("1200"),
      contractMooveBalance = BigDecimal.ZERO
    )

    assertEquals(0, snapshot.amountToAcquire.compareTo(BigDecimal.ZERO))
    assertEquals(0, snapshot.recommendedTopUp.compareTo(BigDecimal("1000")))
  }

  @Test
  fun `contract balance never reduces the caller top up recommendation`() {
    val snapshot = RecoveryPlanCalculator.create(
      claimableRewards = BigDecimal("1000"),
      walletMooveBalance = BigDecimal.ZERO,
      contractMooveBalance = BigDecimal("999999999")
    )

    assertEquals(0, snapshot.amountToAcquire.compareTo(BigDecimal("1000")))
    assertEquals(0, snapshot.recommendedTopUp.compareTo(BigDecimal("1000")))
  }
}
