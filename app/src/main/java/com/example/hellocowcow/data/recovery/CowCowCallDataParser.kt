package com.example.hellocowcow.data.recovery

import java.nio.charset.StandardCharsets
import java.util.Base64

object CowCowCallDataParser {

  fun parseNonceArguments(data: String?, function: String): List<String> {
    val decoded = decodeTransactionData(data) ?: return emptyList()
    val parts = decoded.split('@')
    if (parts.firstOrNull() != function) return emptyList()

    return parts.drop(1).mapNotNull { raw ->
      raw.lowercase().takeIf { nonce ->
        nonce.matches(Regex("[0-9a-f]{4}")) && nonce != "0000"
      }
    }
  }

  fun decodeTransactionData(data: String?): String? {
    val value = data?.takeIf { it.isNotBlank() } ?: return null
    if (value.contains('@') || value.all { it.isLetter() }) return value

    return runCatching {
      String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)
    }.getOrNull()?.takeIf { decoded -> decoded.isNotBlank() }
  }
}
