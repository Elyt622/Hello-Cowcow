package com.example.hellocowcow.data.recovery

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test

class CowCowCallDataParserTest {

  @Test
  fun `parses historical unstake payload`() {
    assertEquals(
      listOf("0788", "0eb2", "267e", "0fde"),
      CowCowCallDataParser.parseNonceArguments(
        "unstake@0788@0eb2@267e@0fde",
        "unstake"
      )
    )
  }

  @Test
  fun `parses base64 API transaction data`() {
    val encoded = Base64.getEncoder().encodeToString(
      "claim@0788@0eb2@267e@0fde".toByteArray()
    )

    assertEquals(
      listOf("0788", "0eb2", "267e", "0fde"),
      CowCowCallDataParser.parseNonceArguments(encoded, "claim")
    )
  }

  @Test
  fun `does not mix claim and claimRewards`() {
    assertEquals(
      emptyList<String>(),
      CowCowCallDataParser.parseNonceArguments("claimRewards", "claim")
    )
  }
}
