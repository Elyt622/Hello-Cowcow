package com.example.hellocowcow.domain.recovery

data class CowCowUnstakePreview(
  val cowNonces: List<String>,
  val payload: String,
  val fee: TransactionFeeEstimate,
  val guardedAccount: Boolean
) {
  val cowCount: Int get() = cowNonces.size
}
