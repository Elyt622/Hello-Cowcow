package com.example.hellocowcow.domain.recovery

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class RecoveryPlanCalculatorTest {

  @Test
  fun `contract balance reduces the temporary top up needed for claim`() {
    val snapshot = RecoveryPlanCalculator.create(
      claimableRewards = BigDecimal("1000"),
      walletMooveBalance = BigDecimal("250"),
      contractMooveBalance = BigDecimal("600")
    )

    assertEquals(0, snapshot.claimLiquidityGap.compareTo(BigDecimal("400")))
    assertEquals(0, snapshot.recommendedTopUp.compareTo(BigDecimal("400")))
    assertEquals(0, snapshot.amountToAcquire.compareTo(BigDecimal("150")))
  }

  @Test
  fun `nothing must be bought when wallet covers the contract liquidity gap`() {
    val snapshot = RecoveryPlanCalculator.create(
      claimableRewards = BigDecimal("1000"),
      walletMooveBalance = BigDecimal("500"),
      contractMooveBalance = BigDecimal("600")
    )

    assertEquals(0, snapshot.claimLiquidityGap.compareTo(BigDecimal("400")))
    assertEquals(0, snapshot.amountToAcquire.compareTo(BigDecimal.ZERO))
    assertEquals(0, snapshot.recommendedTopUp.compareTo(BigDecimal("400")))
  }

  @Test
  fun `no top up is needed when contract already covers the claim`() {
    val snapshot = RecoveryPlanCalculator.create(
      claimableRewards = BigDecimal("1000"),
      walletMooveBalance = BigDecimal.ZERO,
      contractMooveBalance = BigDecimal("1200")
    )

    assertEquals(0, snapshot.claimLiquidityGap.compareTo(BigDecimal.ZERO))
    assertEquals(0, snapshot.amountToAcquire.compareTo(BigDecimal.ZERO))
    assertEquals(0, snapshot.recommendedTopUp.compareTo(BigDecimal.ZERO))
  }

  @Test
  fun `empty contract falls back to full reward liquidity`() {
    val snapshot = RecoveryPlanCalculator.create(
      claimableRewards = BigDecimal("1000"),
      walletMooveBalance = BigDecimal("100"),
      contractMooveBalance = BigDecimal.ZERO
    )

    assertEquals(0, snapshot.claimLiquidityGap.compareTo(BigDecimal("1000")))
    assertEquals(0, snapshot.amountToAcquire.compareTo(BigDecimal("900")))
    assertEquals(0, snapshot.recommendedTopUp.compareTo(BigDecimal("1000")))
  }

  @Test
  fun `stake nonces normalize to minimal hex bytes without changing order`() {
    val snapshot = RecoveryPlanCalculator.create(
      claimableRewards = BigDecimal.ZERO,
      walletMooveBalance = BigDecimal.ZERO,
      contractMooveBalance = BigDecimal.ZERO,
      stakedCowNonces = listOf("0001", "0788", "0EB2", "267e", "0fde")
    )

    assertEquals(
      listOf("01", "0788", "0eb2", "267e", "0fde"),
      snapshot.stakedCowNonces
    )
  }
}
