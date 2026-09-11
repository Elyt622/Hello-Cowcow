package com.example.hellocowcow.domain.transactions

import com.example.hellocowcow.domain.models.TransactionProcessState
import com.example.hellocowcow.domain.models.TransactionProcessStatus
import com.example.hellocowcow.domain.repositories.TransactionProcessStatusRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionTrackerTest {

  @Test
  fun `pending transaction becomes confirmed`() = runTest {
    val tracker = TransactionTracker(
      FakeRepository(
        listOf(
          status(TransactionProcessState.Pending),
          status(TransactionProcessState.Success)
        )
      )
    )

    val result = tracker.awaitFinalStatus("hash", maxAttempts = 3, delayMillis = 0)

    assertTrue(result is TransactionTracker.Result.Confirmed)
  }

  @Test
  fun `failed process status stops immediately`() = runTest {
    val tracker = TransactionTracker(
      FakeRepository(
        listOf(status(TransactionProcessState.Fail, "user error"))
      )
    )

    val result = tracker.awaitFinalStatus("hash", maxAttempts = 5, delayMillis = 0)

    assertTrue(result is TransactionTracker.Result.Failed)
    assertEquals(
      "user error",
      (result as TransactionTracker.Result.Failed).status.reason
    )
  }

  @Test
  fun `unknown status can later become confirmed`() = runTest {
    val tracker = TransactionTracker(
      FakeRepository(
        listOf(
          status(TransactionProcessState.Unknown),
          status(TransactionProcessState.Success)
        )
      )
    )

    val result = tracker.awaitFinalStatus("hash", maxAttempts = 3, delayMillis = 0)

    assertTrue(result is TransactionTracker.Result.Confirmed)
  }

  @Test
  fun `pending transaction times out after bounded attempts`() = runTest {
    val tracker = TransactionTracker(
      FakeRepository(
        List(3) { status(TransactionProcessState.Pending) }
      )
    )

    val result = tracker.awaitFinalStatus("hash", maxAttempts = 3, delayMillis = 0)

    assertTrue(result is TransactionTracker.Result.TimedOut)
    assertEquals(
      TransactionProcessState.Pending,
      (result as TransactionTracker.Result.TimedOut).lastStatus?.state
    )
  }

  @Test
  fun `transient network error does not fail transaction`() = runTest {
    val tracker = TransactionTracker(
      FakeRepository(
        listOf(
          IllegalStateException("temporary gateway error"),
          status(TransactionProcessState.Pending),
          status(TransactionProcessState.Success)
        )
      )
    )

    val result = tracker.awaitFinalStatus("hash", maxAttempts = 3, delayMillis = 0)

    assertTrue(result is TransactionTracker.Result.Confirmed)
  }

  private fun status(
    state: TransactionProcessState,
    reason: String = ""
  ) = TransactionProcessStatus(state, reason)

  private class FakeRepository(
    private val responses: List<Any>
  ) : TransactionProcessStatusRepository {
    private var index = 0

    override suspend fun getProcessStatus(txHash: String): TransactionProcessStatus {
      val response = responses[index.coerceAtMost(responses.lastIndex)]
      index++
      if (response is Throwable) throw response
      return response as TransactionProcessStatus
    }
  }
}
