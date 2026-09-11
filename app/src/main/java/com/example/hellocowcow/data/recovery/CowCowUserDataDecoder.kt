package com.example.hellocowcow.data.recovery

import com.example.hellocowcow.domain.recovery.CowCowNonceCodec
import java.util.Base64

object CowCowUserDataDecoder {

  fun decodeStakedCowNonces(returnData: String): List<String> {
    require(returnData.isNotBlank()) { "CowCow contract returned empty user data" }

    val bytes = Base64.getDecoder().decode(returnData)
    require(bytes.isNotEmpty()) { "CowCow user data is empty" }

    val hex = bytes.joinToString(separator = "") { byte ->
      "%02x".format(byte.toInt() and 0xff)
    }

    // Match the decoding path historically used by the Staked portfolio screen.
    // getAllDataForUser contains other zero-valued fields before the staking list,
    // so treating the first two bytes as the stake count is not reliable.
    val nonZeroSegments = buildList {
      var index = 0
      while (index < hex.length) {
        val end = minOf(index + 4, hex.length)
        val segment = hex.substring(index, end)
        if (!segment.contains("0000")) {
          add(if (segment.startsWith("00")) segment.drop(2) else segment)
        }
        index += 4
      }
    }

    if (nonZeroSegments.isEmpty()) return emptyList()

    val count = nonZeroSegments.first().toIntOrNull(16)
      ?: throw IllegalArgumentException("CowCow user data does not contain a valid stake count")

    if (count == 0) return emptyList()
    require(nonZeroSegments.size >= count + 1) {
      "CowCow user data ended before all staked nonces could be decoded"
    }

    return nonZeroSegments
      .subList(1, count + 1)
      .map(CowCowNonceCodec::normalize)
  }
}
