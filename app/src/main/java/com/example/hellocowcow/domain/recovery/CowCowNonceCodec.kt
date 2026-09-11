package com.example.hellocowcow.domain.recovery

object CowCowNonceCodec {

  fun normalize(raw: String): String {
    val value = raw.lowercase()
    require(value.isNotBlank() && value.length % 2 == 0) {
      "CowCow nonce must be a non-empty even-length hexadecimal byte string"
    }
    require(value.matches(Regex("[0-9a-f]+"))) {
      "CowCow nonce must contain hexadecimal characters only"
    }

    val normalized = value
      .chunked(2)
      .dropWhile { byte -> byte == "00" }
      .joinToString(separator = "")

    require(normalized.isNotEmpty()) { "CowCow nonce cannot be zero" }
    return normalized
  }
}
