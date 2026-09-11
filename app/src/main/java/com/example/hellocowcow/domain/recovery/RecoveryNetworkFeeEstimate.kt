package com.example.hellocowcow.domain.recovery

import java.math.BigDecimal

data class TransactionFeeEstimate(
  val feeEgld: BigDecimal,
  val maxFeeEgld: BigDecimal,
  val gasUnits: Long,
  val gasLimit: Long,
  val simulated: Boolean
) {
  companion object {
    val ZERO = TransactionFeeEstimate(
      feeEgld = BigDecimal.ZERO,
      maxFeeEgld = BigDecimal.ZERO,
      gasUnits = 0L,
      gasLimit = 0L,
      simulated = true
    )
  }
}

data class RecoveryNetworkFeeEstimate(
  val topUp: TransactionFeeEstimate?,
  val claim: TransactionFeeEstimate,
  val totalFeeEgld: BigDecimal,
  val fullySimulated: Boolean
) {
  val maxTotalFeeEgld: BigDecimal
    get() = (topUp?.maxFeeEgld ?: BigDecimal.ZERO).add(claim.maxFeeEgld)
}
