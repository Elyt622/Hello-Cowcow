package com.example.hellocowcow.domain.models

data class RecoveryUnbondBatch(
  val unstakeTxHash: String,
  val cowNonces: List<String>,
  val unstakedAtEpochSeconds: Long,
  val claimableAtEpochSeconds: Long
) {
  fun isReady(nowEpochSeconds: Long): Boolean = nowEpochSeconds >= claimableAtEpochSeconds
}
