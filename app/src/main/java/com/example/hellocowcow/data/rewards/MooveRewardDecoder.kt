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

    val decoded = runCatching {
      java.util.Base64.getDecoder().decode(returnData)
    }.getOrNull()

    if (decoded != null) {
      return decodeStructuredRewardAmount(decoded)
        .toBigDecimal(TOKEN_DECIMALS)
    }

    // Compatibility path for historical fixtures / legacy responses that are not
    // the complete Base64-encoded getAllDataForUser payload.
    decodeLegacyRewardAmount(returnData)?.let { amount ->
      return amount.toBigDecimal(TOKEN_DECIMALS)
    }

    throw IllegalArgumentException(
      "Unable to locate the MOOVE reward amount in contract data"
    )
  }

  private fun decodeStructuredRewardAmount(bytes: ByteArray): BigInteger {
    if (bytes.size < NESTED_LENGTH_BYTES) {
      throw IllegalArgumentException(
        "CowCow reward payload is too short"
      )
    }

    val candidates = collectNestedBigUintCandidates(bytes)
      .filter { it.value > BigInteger.ZERO }

    if (candidates.isEmpty()) {
      if (hasNestedZeroAtEnd(bytes)) {
        return BigInteger.ZERO
      }

      throw IllegalArgumentException(
        "Unable to locate the MOOVE reward BigUint in contract data"
      )
    }

    // MOOVE is denominated to 18 decimals, so the reward amount normally has the
    // largest BigUint payload among the compact metadata fields returned by
    // getAllDataForUser. This avoids the old regex bug where another nearby integer
    // (or a trailing zero field) could be mistaken for the claimable reward.
    val maxPayloadLength = candidates.maxOf { it.payloadLength }
    val longest = candidates
      .filter { it.payloadLength == maxPayloadLength }
      .distinctBy { it.value }

    if (longest.size != 1) {
      throw IllegalArgumentException(
        "Ambiguous CowCow reward data: multiple equally-sized BigUint values were found"
      )
    }

    return longest.single().value
  }

  private fun collectNestedBigUintCandidates(
    bytes: ByteArray
  ): List<NestedBigUintCandidate> {
    val candidates = mutableListOf<NestedBigUintCandidate>()

    for (offset in 0..bytes.size - NESTED_LENGTH_BYTES) {
      val payloadLength = readUnsignedInt(bytes, offset)

      if (
        payloadLength <= 0L ||
        payloadLength > MAX_REASONABLE_BIGUINT_BYTES.toLong()
      ) {
        continue
      }

      val payloadStart = offset + NESTED_LENGTH_BYTES
      val payloadEnd = payloadStart + payloadLength.toInt()
      if (payloadEnd > bytes.size) continue

      val value = BigInteger(
        1,
        bytes.copyOfRange(payloadStart, payloadEnd)
      )

      candidates += NestedBigUintCandidate(
        payloadLength = payloadLength.toInt(),
        value = value
      )
    }

    return candidates
  }

  private fun hasNestedZeroAtEnd(bytes: ByteArray): Boolean {
    if (bytes.size < NESTED_LENGTH_BYTES) return false
    val offset = bytes.size - NESTED_LENGTH_BYTES
    return readUnsignedInt(bytes, offset) == 0L
  }

  private fun decodeLegacyRewardAmount(returnData: String): BigInteger? {
    val encodedAmount = encodedAmountPattern
      .findAll(returnData.takeLast(LEGACY_SEARCH_WINDOW))
      .lastOrNull()
      ?.value
      ?: return null

    val decoded = Base64.decodeBase64(encodedAmount)
    if (decoded.size <= 1) return null

    return BigInteger(
      1,
      decoded.copyOfRange(1, decoded.size)
    )
  }

  private fun readUnsignedInt(bytes: ByteArray, offset: Int): Long {
    return ((bytes[offset].toLong() and 0xffL) shl 24) or
        ((bytes[offset + 1].toLong() and 0xffL) shl 16) or
        ((bytes[offset + 2].toLong() and 0xffL) shl 8) or
        (bytes[offset + 3].toLong() and 0xffL)
  }

  private data class NestedBigUintCandidate(
    val payloadLength: Int,
    val value: BigInteger
  )
}
