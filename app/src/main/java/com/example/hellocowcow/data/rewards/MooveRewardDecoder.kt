package com.example.hellocowcow.data.rewards

import io.ipfs.multibase.binary.Base64
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object MooveRewardDecoder {

  private const val TOKEN_DECIMALS = 18
  private const val DISPLAY_SCALE = 4
  private const val LEGACY_SEARCH_WINDOW = 35

  private val encodedAmountPattern = Regex(
    "B[0-9w-z+/][A-Za-z0-9+/]{8}A|" +
        "C[A-P][A-Za-z0-9+/]{9}AA|" +
        "C[Q-Za-f][A-Za-z0-9+/]{10}AA|" +
        "C[g-v][A-Za-z0-9+/]{11}AA"
  )

  fun decodeClaimableAmount(returnData: String): BigDecimal {
    require(returnData.isNotBlank()) {
      "Rewards contract returned empty data"
    }

    val encodedAmount = encodedAmountPattern
      .find(returnData.takeLast(LEGACY_SEARCH_WINDOW))
      ?.value
      ?: throw IllegalArgumentException(
        "Unable to locate the MOOVE reward amount in contract data"
      )

    val decoded = Base64.decodeBase64(encodedAmount)
    require(decoded.size > 1) {
      "MOOVE reward payload is too short"
    }

    // The legacy CowCow contract response prefixes the integer with its encoded length.
    // Preserve that wire format while parsing the amount directly as an unsigned integer.
    val amount = BigInteger(
      1,
      decoded.copyOfRange(1, decoded.size)
    )

    return amount
      .toBigDecimal(TOKEN_DECIMALS)
      .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
  }
}
