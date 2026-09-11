package com.example.hellocowcow.domain.recovery

import com.example.hellocowcow.domain.models.MvxTransaction

data class CowCowUnstakePreview(
  val cowNonces: List<String>,
  val payload: String,
  val transaction: MvxTransaction,
  val fee: TransactionFeeEstimate
) {
  val cowCount: Int get() = cowNonces.size
}
