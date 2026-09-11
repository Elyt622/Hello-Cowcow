package com.example.hellocowcow.domain.recovery

data class CowCowUnstakePreview(
  val cowNonces: List<String>,
  val payload: String,
  val historicalGasLimit: Long,
  val guardedAccount: Boolean
) {
  val cowCount: Int get() = cowNonces.size
}
