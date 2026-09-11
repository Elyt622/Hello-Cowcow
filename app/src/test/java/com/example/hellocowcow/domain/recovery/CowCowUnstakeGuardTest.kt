package com.example.hellocowcow.domain.recovery

import com.example.hellocowcow.domain.models.RecoverySnapshot
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CowCowUnstakeGuardTest {

  @Test
  fun `allows direct unstake when reviewed nonces are unchanged and contract covers rewards`() {
    val snapshot = snapshot(
      rewards = "1250",
      contract = "1300",
      gap = "0",
      nonces = listOf("0788", "0eb2")
    )

    val result = CowCowUnstakeGuard.validate(
      expectedNonces = listOf("0788", "0eb2"),
      latestSnapshot = snapshot,
      latestNonces = listOf("0788", "0eb2")
    )

    assertTrue(result is CowCowUnstakeGuard.Result.Ready)
    result as CowCowUnstakeGuard.Result.Ready
    assertEquals("unstake@0788@0eb2", result.payload)
    assertEquals(BigDecimal("1250"), result.rewardsMoove)
  }

  @Test
  fun `blocks when contract liquidity no longer covers the reward payout`() {
    val result = CowCowUnstakeGuard.validate(
      expectedNonces = listOf("0788"),
      latestSnapshot = snapshot(
        rewards = "1250",
        contract = "1000",
        gap = "250",
        nonces = listOf("0788")
      ),
      latestNonces = listOf("0788")
    )

    assertTrue(result is CowCowUnstakeGuard.Result.Blocked)
    result as CowCowUnstakeGuard.Result.Blocked
    assertTrue(result.reason.contains("250 MOOVE"))
  }

  @Test
  fun `blocks when CowCow nonce state changed after user review`() {
    val result = CowCowUnstakeGuard.validate(
      expectedNonces = listOf("0788", "0eb2"),
      latestSnapshot = snapshot(
        rewards = "0",
        contract = "59.1784",
        gap = "0",
        nonces = listOf("0788")
      ),
      latestNonces = listOf("0788")
    )

    assertTrue(result is CowCowUnstakeGuard.Result.Blocked)
    result as CowCowUnstakeGuard.Result.Blocked
    assertTrue(result.reason.contains("changed"))
  }

  private fun snapshot(
    rewards: String,
    contract: String,
    gap: String,
    nonces: List<String>
  ) = RecoverySnapshot(
    claimableRewards = BigDecimal(rewards),
    walletMooveBalance = BigDecimal.ZERO,
    contractMooveBalance = BigDecimal(contract),
    claimLiquidityGap = BigDecimal(gap),
    amountToAcquire = BigDecimal.ZERO,
    recommendedTopUp = BigDecimal(gap),
    stakedCowNonces = nonces
  )
}
