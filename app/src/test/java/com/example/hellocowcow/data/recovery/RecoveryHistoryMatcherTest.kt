package com.example.hellocowcow.data.recovery

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.retrofit.mvxApi.response.Transactions
import com.example.hellocowcow.domain.recovery.RecoveryEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryHistoryMatcherTest {

  @Test
  fun `verified July 2026 unstake is cleared by matching final claim`() {
    val unstake = transaction(
      hash = "e9455f080ad8ca3604b503fea5204845c174031edf079780401d8a906b7b415a",
      timestamp = 1_783_949_916,
      data = "unstake@0788@0eb2@267e@0fde"
    )
    val finalClaim = transaction(
      hash = "929157471e148b5d092a1e90d6b8638974b0ef5da6d012f97bf7e303cc1d9ee8",
      timestamp = 1_784_557_524,
      data = "claim@0788@0eb2@267e@0fde"
    )

    assertTrue(
      RecoveryHistoryMatcher.pendingBatches(
        unstakeTransactions = listOf(unstake),
        finalClaimTransactions = listOf(finalClaim)
      ).isEmpty()
    )
  }

  @Test
  fun `pending batch uses observed successful final claim delay conservatively`() {
    val unstake = transaction(
      hash = "e9455f080ad8ca3604b503fea5204845c174031edf079780401d8a906b7b415a",
      timestamp = 1_783_949_916,
      data = "unstake@0788@0eb2@267e@0fde"
    )

    val batch = RecoveryHistoryMatcher.pendingBatches(
      unstakeTransactions = listOf(unstake),
      finalClaimTransactions = emptyList()
    ).single()

    assertEquals(listOf("0788", "0eb2", "267e", "0fde"), batch.cowNonces)
    assertEquals(1_783_949_916L, batch.unstakedAtEpochSeconds)
    assertEquals(1_784_557_524L, batch.claimableAtEpochSeconds)
    assertEquals(
      RecoveryEvidence.OBSERVED_SUCCESSFUL_FINAL_CLAIM_DELAY_SECONDS,
      batch.claimableAtEpochSeconds - batch.unstakedAtEpochSeconds
    )
  }

  @Test
  fun `partial final claim leaves only unclaimed CowCow nonces pending`() {
    val unstake = transaction(
      hash = "unstake-hash",
      timestamp = 1_783_949_916,
      data = "unstake@0788@0eb2@267e@0fde"
    )
    val partialClaim = transaction(
      hash = "claim-hash",
      timestamp = 1_784_557_524,
      data = "claim@0788@267e"
    )

    val batch = RecoveryHistoryMatcher.pendingBatches(
      unstakeTransactions = listOf(unstake),
      finalClaimTransactions = listOf(partialClaim)
    ).single()

    assertEquals(listOf("0eb2", "0fde"), batch.cowNonces)
  }

  private fun transaction(
    hash: String,
    timestamp: Int,
    data: String
  ): Transactions = Transactions(
    txHash = hash,
    receiver = CowCowConfig.REWARDS_CONTRACT,
    status = "success",
    timestamp = timestamp,
    data = data
  )
}
