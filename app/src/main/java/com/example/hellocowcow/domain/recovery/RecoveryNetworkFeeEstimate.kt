package com.example.hellocowcow.domain.recovery

import java.math.BigDecimal

data class TransactionFeeEstimate(
  val feeEgld: BigDecimal,
  val gasUnits: Long,
  val simulated: Boolean
)

data class RecoveryNetworkFeeEstimate(
  val topUp: TransactionFeeEstimate?,
  val claim: TransactionFeeEstimate,
  val totalFeeEgld: BigDecimal,
  val fullySimulated: Boolean
)
