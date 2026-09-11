package com.example.hellocowcow.data.recovery

import java.util.Base64

object CowCowUserDataDecoder {

  fun decodeStakedCowNonces(returnData: String): List<String> {
    require(returnData.isNotBlank()) { "CowCow contract returned empty user data" }

    val bytes = Base64.getDecoder().decode(returnData)
    require(bytes.size >= 2) { "CowCow user data is too short" }

    val hex = bytes.joinToString(separator = "") { byte ->
      "%02x".format(byte.toInt() and 0xff)
    }
    require(hex.length >= 4) { "CowCow user data does not contain a stake count" }

    val count = hex.substring(0, 4).toInt(16)
    if (count == 0) return emptyList()

    val requiredLength = 4 + count * 4
    require(hex.length >= requiredLength) {
      "CowCow user data ended before all staked nonces could be decoded"
    }

    return buildList(count) {
      repeat(count) { index ->
        val start = 4 + index * 4
        val nonce = hex.substring(start, start + 4)
        require(nonce != "0000") { "CowCow user data contains an invalid zero nonce" }
        add(nonce)
      }
    }
  }
}
