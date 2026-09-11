package com.example.hellocowcow.data.transaction

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.retrofit.mvxApi.request.Transaction
import com.example.hellocowcow.domain.models.DomainAccount

object ClaimTransactionFactory {

  private const val GUARDED_TRANSACTION_OPTION = 2

  fun create(account: DomainAccount): Transaction {
    val guardian = if (account.isGuarded) {
      require(account.activeGuardianAddress.isNotBlank()) {
        "Guarded account is missing its active guardian address"
      }
      account.activeGuardianAddress
    } else {
      null
    }

    return Transaction(
      nonce = account.nonce,
      value = "0",
      receiver = CowCowConfig.REWARDS_CONTRACT,
      sender = account.address,
      gasPrice = CowCowConfig.MIN_GAS_PRICE,
      gasLimit = CowCowConfig.CLAIM_REWARDS_GAS_LIMIT +
          if (account.isGuarded) CowCowConfig.GUARDED_TRANSACTION_GAS_OVERHEAD else 0L,
      data = CowCowConfig.CLAIM_REWARDS_DATA,
      chainID = CowCowConfig.MAINNET_CHAIN_ID,
      version = if (account.isGuarded) 2 else 1,
      options = if (account.isGuarded) GUARDED_TRANSACTION_OPTION else null,
      guardian = guardian
    )
  }
}
