package com.example.hellocowcow.domain.models

data class MvxTransaction(
  val nonce: Int = 0,
  val value: String = "0",
  val receiver: String = "",
  val sender: String = "",
  val gasPrice: Long = 0,
  val gasLimit: Long = 0,
  val data: String? = null,
  val chainID: String = "1",
  val version: Int = 1,
  val signature: String? = null,
  val options: Int? = null,
  val guardian: String? = null,
  val guardianSignature: String? = null
)
