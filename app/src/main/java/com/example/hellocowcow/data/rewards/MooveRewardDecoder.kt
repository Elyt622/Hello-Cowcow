package com.example.hellocowcow.data.rewards

import io.ipfs.multibase.binary.Base64
import java.math.BigDecimal
import java.math.BigInteger

object MooveRewardDecoder {

  private const val TOKEN_DECIMALS = 18
  private const val LEGACY_SEARCH_WINDOW = 35
  private const val NESTED_LENGTH_BYTES = 4
  private const val MAX_REASONABLE_BIGUINT_BYTES = 64

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

    // This is the extraction path used by the original working CowCow app.
    // Prefer it for non-zero rewards because getAllDataForUser contains several
    // nested integers near the end of the payload and a generic suffix decoder
    // can otherwise lock onto the wrong small field.
    decodeLegacyRewardAmount(returnData)?.let { amount ->
      return amount.toBigDecimal(TOKEN_DECIMALS)
    }

    // Keep structural decoding as a zero-safe fallback for payloads where the
    // legacy regex has no amount token (notably after rewards have been claimed).
    decodeNestedAmountAtEnd(returnData)?.let { amount ->
      return amount.toBigDecimal(TOKEN_DECIMALS)
    }

    throw IllegalArgumentException(
      "Unable to locate the MOOVE reward amount in contract data"
    )
  }

  private fun decodeLegacyRewardAmount(returnData: String): BigInteger? {
    val encodedAmount = encodedAmountPattern
      .find(returnData.takeLast(LEGACY_SEARCH_WINDOW))
      ?.value
      ?: return null

    val decoded = Base64.decodeBase64(encodedAmount)
    if (decoded.size <= 1) return null

    return BigInteger(
      1,
      decoded.copyOfRange(1, decoded.size)
    )
  }

  private fun decodeNestedAmountAtEnd(returnData: String): BigInteger? {
    val bytes = runCatching {
      java.util.Base64.getDecoder().decode(returnData)
    }.getOrNull() ?: return null

    if (bytes.size < NESTED_LENGTH_BYTES) return null

    val maxLength = minOf(
      MAX_REASONABLE_BIGUINT_BYTES,
      bytes.size - NESTED_LENGTH_BYTES
    )

    for (payloadLength in maxLength downTo 0) {
      val prefixStart = bytes.size - NESTED_LENGTH_BYTES - payloadLength
      val declaredLength = readUnsignedInt(bytes, prefixStart)
      if (declaredLength != payloadLength.toLong()) continue

      if (payloadLength == 0) return BigInteger.ZERO

      return BigInteger(
        1,
        bytes.copyOfRange(prefixStart + NESTED_LENGTH_BYTES, bytes.size)
      )
    }

    return null
  }

  private fun readUnsignedInt(bytes: ByteArray, offset: Int): Long {
    return ((bytes[offset].toLong() and 0xffL) shl 24) or
        ((bytes[offset + 1].toLong() and 0xffL) shl 16) or
        ((bytes[offset + 2].toLong() and 0xffL) shl 8) or
        (bytes[offset + 3].toLong() and 0xffL)
  }
}
