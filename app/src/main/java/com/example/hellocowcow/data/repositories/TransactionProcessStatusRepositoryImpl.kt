package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.data.network.api.MvxGatewayApi
import com.example.hellocowcow.domain.models.TransactionProcessState
import com.example.hellocowcow.domain.models.TransactionProcessStatus
import com.example.hellocowcow.domain.repositories.TransactionProcessStatusRepository
import javax.inject.Inject

class TransactionProcessStatusRepositoryImpl @Inject constructor(
  private val gatewayApi: MvxGatewayApi
) : TransactionProcessStatusRepository {

  override suspend fun getProcessStatus(txHash: String): TransactionProcessStatus {
    require(txHash.isNotBlank()) { "Transaction hash cannot be blank" }

    val response = gatewayApi.getProcessStatus(txHash)
    if (response.data == null) {
      error(response.error.ifBlank { "MultiversX Gateway returned no process status" })
    }

    val state = when (response.data.status.lowercase()) {
      "pending" -> TransactionProcessState.Pending
      "success" -> TransactionProcessState.Success
      "fail" -> TransactionProcessState.Fail
      else -> TransactionProcessState.Unknown
    }

    return TransactionProcessStatus(
      state = state,
      reason = response.data.reason
    )
  }
}
