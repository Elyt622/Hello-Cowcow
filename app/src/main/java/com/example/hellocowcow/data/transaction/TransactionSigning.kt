package com.example.hellocowcow.data.transaction

import com.example.hellocowcow.core.wallet.MvxSignTransactionResult
import com.example.hellocowcow.data.retrofit.mvxApi.request.Transaction

fun Transaction.withWalletResult(
  result: MvxSignTransactionResult
): Transaction = copy(
  signature = result.signature,
  guardian = result.guardian ?: guardian,
  guardianSignature = result.guardianSignature ?: guardianSignature,
  options = result.options ?: options,
  version = result.version ?: version
)
