package com.example.hellocowcow.domain.transactions

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.models.MvxTransaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.util.Base64

object RecoveryTopUpTransactionFactory {

  private const val GUARDED_TRANSACTION_OPTION = 2

  fun create(
    account: DomainAccount,
    amountMoove: BigDecimal
  ): MvxTransaction {
    require(amountMoove > BigDecimal.ZERO) { "MOOVE top-up amount must be positive" }

    val atomicAmount = try {
      amountMoove
        .movePointRight(CowCowConfig.MOOVE_DECIMALS)
        .setScale(0, RoundingMode.UNNECESSARY)
        .toBigIntegerExact()
    } catch (error: ArithmeticException) {
      throw IllegalArgumentException(
        "MOOVE amount supports at most ${CowCowConfig.MOOVE_DECIMALS} decimal places",
        error
      )
    }

    val tokenHex = CowCowConfig.MOOVE_TOKEN_ID.toHexUtf8()
    val rawAmountHex = atomicAmount.toString(16)
    val amountHex = if (rawAmountHex.length % 2 == 0) rawAmountHex else "0$rawAmountHex"
    val payload = "ESDTTransfer@$tokenHex@$amountHex"
    val dataBase64 = Base64.getEncoder().encodeToString(
      payload.toByteArray(StandardCharsets.UTF_8)
    )

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
      gasLimit = CowCowConfig.ESDT_TRANSFER_GAS_LIMIT +
          if (account.isGuarded) CowCowConfig.GUARDED_TRANSACTION_GAS_OVERHEAD else 0L,
      data = dataBase64,
      chainID = CowCowConfig.MAINNET_CHAIN_ID,
      version = if (account.isGuarded) 2 else 1,
      options = if (account.isGuarded) GUARDED_TRANSACTION_OPTION else null,
      guardian = guardian
    )
  }

  private fun String.toHexUtf8(): String = toByteArray(StandardCharsets.UTF_8)
    .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
