package com.example.hellocowcow.domain.transactions

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.models.MvxTransaction
import com.example.hellocowcow.domain.recovery.CowCowUnstakePreviewCodec

object CowCowUnstakeTransactionFactory {

  private const val GUARDED_TRANSACTION_OPTION = 2

  fun create(
    account: DomainAccount,
    cowNonces: List<String>
  ): MvxTransaction {
    val encoded = CowCowUnstakePreviewCodec.encode(cowNonces)

    val guardian = if (account.isGuarded) {
      require(account.activeGuardianAddress.isNotBlank()) {
        "Guarded account is missing its active guardian address"
      }
      account.activeGuardianAddress
    } else {
      null
    }

    return MvxTransaction(
      nonce = account.nonce,
      value = "0",
      receiver = CowCowConfig.REWARDS_CONTRACT,
      sender = account.address,
      gasPrice = CowCowConfig.MIN_GAS_PRICE,
      gasLimit = CowCowConfig.UNSTAKE_GAS_LIMIT +
          if (account.isGuarded) CowCowConfig.GUARDED_TRANSACTION_GAS_OVERHEAD else 0L,
      data = encoded.dataBase64,
      chainID = CowCowConfig.MAINNET_CHAIN_ID,
      version = if (account.isGuarded) 2 else 1,
      options = if (account.isGuarded) GUARDED_TRANSACTION_OPTION else null,
      guardian = guardian
    )
  }
}
