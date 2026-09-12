// domain/transactions/CowCowUnstakeTransactionFactory.kt

package com.example.hellocowcow.domain.transactions

import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.models.MvxTransaction

object CowCowUnstakeTransactionFactory {

  fun create(
    account: DomainAccount,
    cowNonces: List<String>
  ): MvxTransaction {
    require(cowNonces.isNotEmpty()) {
      "At least one CowCow is required"
    }

    /*
     * 1. Encode cowNonces with CowCowUnstakePreviewCodec
     * 2. Build the MultiversX transaction targeting the CowCow contract
     * 3. Use the account nonce / guarded-account metadata
     * 4. Return the unsigned MvxTransaction
     *
     * This is the single executable hook still to implement.
     */

    error("CowCow unstake transaction builder not wired")
  }
}