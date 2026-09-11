package com.example.hellocowcow.data.transaction

import com.example.hellocowcow.data.retrofit.mvxApi.request.Transaction
import com.example.hellocowcow.domain.models.MvxTransaction

internal fun MvxTransaction.toRequest(): Transaction = Transaction(
  nonce = nonce,
  value = value,
  receiver = receiver,
  sender = sender,
  gasPrice = gasPrice,
  gasLimit = gasLimit,
  data = data,
  chainID = chainID,
  version = version,
  signature = signature,
  options = options,
  guardian = guardian,
  guardianSignature = guardianSignature
)
