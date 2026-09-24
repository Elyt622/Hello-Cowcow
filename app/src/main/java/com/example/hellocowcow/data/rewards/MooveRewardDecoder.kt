package com.example.hellocowcow.data.rewards

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.recovery.CowCowUserDataDecoder
import java.math.BigDecimal
import java.math.BigInteger

object MooveRewardDecoder {

  fun decodeClaimableAmount(returnData: String): BigDecimal {
    val payments = CowCowUserDataDecoder.decode(returnData).rewards
      .filter { it.tokenIdentifier == CowCowConfig.MOOVE_TOKEN_ID }

    require(payments.all { it.tokenNonce == BigInteger.ZERO }) {
      "Unexpected nonce for fungible MOOVE rewards"
    }

    // Select the MOOVE payment by identifier, never by size, magnitude or list position.
    // An empty (successfully decoded) MOOVE reward list is a real zero, not a fallback.
    val atomicAmount = payments.fold(BigInteger.ZERO) { total, payment ->
      total.add(payment.amount)
    }
    return atomicAmount.toBigDecimal(CowCowConfig.MOOVE_DECIMALS)
  }
}
