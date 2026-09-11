package com.example.hellocowcow.domain.recovery

import com.example.hellocowcow.core.config.CowCowConfig
import java.nio.charset.StandardCharsets
import java.util.Base64

object CowCowFinalClaimPreviewCodec {

  data class Encoded(
    val nonces: List<String>,
    val payload: String,
    val dataBase64: String
  )

  fun encode(cowNonces: List<String>): Encoded {
    require(cowNonces.isNotEmpty()) { "At least one unbonded CowCow is required" }

    val normalized = cowNonces.map(::normalizeNonce)
    require(normalized.distinct().size == normalized.size) {
      "CowCow final claim preview contains duplicate nonces"
    }

    val payload = buildString {
      append(CowCowConfig.FINAL_CLAIM_FUNCTION)
      normalized.forEach { nonce ->
        append('@')
        append(nonce)
      }
    }

    return Encoded(
      nonces = normalized,
      payload = payload,
      dataBase64 = Base64.getEncoder().encodeToString(
        payload.toByteArray(StandardCharsets.UTF_8)
      )
    )
  }

  private fun normalizeNonce(raw: String): String {
    val value = raw.trim().lowercase()
    require(value.isNotBlank() && value.length % 2 == 0) {
      "CowCow nonce must be a non-empty even-length hexadecimal value"
    }
    require(value.matches(Regex("[0-9a-f]+"))) {
      "CowCow nonce must contain hexadecimal characters only"
    }

    val normalized = value
      .chunked(2)
      .dropWhile { it == "00" }
      .joinToString(separator = "")

    require(normalized.isNotEmpty()) { "CowCow nonce cannot be zero" }
    return normalized
  }
}
