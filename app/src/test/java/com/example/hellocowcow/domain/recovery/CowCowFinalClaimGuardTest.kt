package com.example.hellocowcow.domain.recovery

import com.example.hellocowcow.domain.models.RecoveryUnbondBatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CowCowFinalClaimGuardTest {

  @Test
  fun `allows exact pending batch at conservative claim threshold`() {
    val batch = batch(
      hash = "unstake-hash",
      nonces = listOf("0788", "0eb2"),
      claimableAt = 10_000L
    )

    val result = CowCowFinalClaimGuard.validate(
      expectedBatch = batch,
      latestPendingBatches = listOf(batch),
      nowEpochSeconds = 10_000L
    )

    assertTrue(result is CowCowFinalClaimGuard.Result.Ready)
    result as CowCowFinalClaimGuard.Result.Ready
    assertEquals(listOf("0788", "0eb2"), result.nonces)
    assertEquals("claim@0788@0eb2", result.payload)
  }

  @Test
  fun `blocks final claim before conservative threshold`() {
    val batch = batch(
      hash = "unstake-hash",
      nonces = listOf("0788"),
      claimableAt = 10_000L
    )

    val result = CowCowFinalClaimGuard.validate(
      expectedBatch = batch,
      latestPendingBatches = listOf(batch),
      nowEpochSeconds = 9_900L
    )

    assertTrue(result is CowCowFinalClaimGuard.Result.Blocked)
    result as CowCowFinalClaimGuard.Result.Blocked
    assertTrue(result.reason.contains("100 seconds"))
  }

  @Test
  fun `blocks when selected unstake batch is no longer pending`() {
    val expected = batch(
      hash = "unstake-hash",
      nonces = listOf("0788"),
      claimableAt = 10_000L
    )

    val result = CowCowFinalClaimGuard.validate(
      expectedBatch = expected,
      latestPendingBatches = emptyList(),
      nowEpochSeconds = 11_000L
    )

    assertTrue(result is CowCowFinalClaimGuard.Result.Blocked)
    result as CowCowFinalClaimGuard.Result.Blocked
    assertTrue(result.reason.contains("no longer pending"))
  }

  @Test
  fun `blocks when pending nonce state changed after review`() {
    val expected = batch(
      hash = "unstake-hash",
      nonces = listOf("0788", "0eb2"),
      claimableAt = 10_000L
    )
    val latest = expected.copy(cowNonces = listOf("0eb2"))

    val result = CowCowFinalClaimGuard.validate(
      expectedBatch = expected,
      latestPendingBatches = listOf(latest),
      nowEpochSeconds = 11_000L
    )

    assertTrue(result is CowCowFinalClaimGuard.Result.Blocked)
    result as CowCowFinalClaimGuard.Result.Blocked
    assertTrue(result.reason.contains("changed"))
  }

  private fun batch(
    hash: String,
    nonces: List<String>,
    claimableAt: Long
  ) = RecoveryUnbondBatch(
    unstakeTxHash = hash,
    cowNonces = nonces,
    unstakedAtEpochSeconds = 1_000L,
    claimableAtEpochSeconds = claimableAt
  )
}
