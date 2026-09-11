package com.example.hellocowcow.data.recovery

import com.example.hellocowcow.domain.recovery.CowCowNonceCodec
import java.nio.charset.StandardCharsets
import java.util.Base64

object CowCowCallDataParser {

  fun parseNonceArguments(data: String?, function: String): List<String> {
    val decoded = decodeTransactionData(data) ?: return emptyList()
    val parts = decoded.split('@')
    if (parts.firstOrNull() != function) return emptyList()

    val rawArguments = parts.drop(1)
    if (rawArguments.isEmpty()) return emptyList()

    return runCatching {
      rawArguments.map(CowCowNonceCodec::normalize)
    }.getOrDefault(emptyList())
  }

  fun decodeTransactionData(data: String?): String? {
    val value = data?.takeIf { it.isNotBlank() } ?: return null
    if (value.contains('@') || value.all { it.isLetter() }) return value

    return runCatching {
      String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)
    }.getOrNull()?.takeIf { decoded -> decoded.isNotBlank() }
  }
}
