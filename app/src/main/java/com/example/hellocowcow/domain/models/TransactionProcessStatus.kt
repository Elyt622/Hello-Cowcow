package com.example.hellocowcow.domain.models

enum class TransactionProcessState {
  Pending,
  Success,
  Fail,
  Unknown
}

data class TransactionProcessStatus(
  val state: TransactionProcessState,
  val reason: String = ""
)
