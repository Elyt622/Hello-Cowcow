package com.example.hellocowcow.domain.transactions

import com.example.hellocowcow.domain.models.TransactionProcessState
import com.example.hellocowcow.domain.models.TransactionProcessStatus
import com.example.hellocowcow.domain.repositories.TransactionProcessStatusRepository
import javax.inject.Inject
import kotlinx.coroutines.delay

class TransactionTracker @Inject constructor(
  private val repository: TransactionProcessStatusRepository
) {

  sealed interface Result {
    data class Confirmed(val status: TransactionProcessStatus) : Result
    data class Failed(val status: TransactionProcessStatus) : Result
    data class TimedOut(val lastStatus: TransactionProcessStatus?) : Result
  }

  suspend fun awaitFinalStatus(
    txHash: String,
    maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    delayMillis: Long = DEFAULT_DELAY_MILLIS
  ): Result {
    require(txHash.isNotBlank()) { "Transaction hash cannot be blank" }
    require(maxAttempts > 0) { "maxAttempts must be positive" }
    require(delayMillis >= 0) { "delayMillis cannot be negative" }

    var lastStatus: TransactionProcessStatus? = null

    repeat(maxAttempts) { attempt ->
      val status = runCatching {
        repository.getProcessStatus(txHash)
      }.getOrNull()

      if (status != null) {
        lastStatus = status
        when (status.state) {
          TransactionProcessState.Success -> return Result.Confirmed(status)
          TransactionProcessState.Fail -> return Result.Failed(status)
          TransactionProcessState.Pending,
          TransactionProcessState.Unknown -> Unit
        }
      }

      if (attempt < maxAttempts - 1 && delayMillis > 0) {
        delay(delayMillis)
      }
    }

    return Result.TimedOut(lastStatus)
  }

  private companion object {
    const val DEFAULT_MAX_ATTEMPTS = 30
    const val DEFAULT_DELAY_MILLIS = 2_000L
  }
}
