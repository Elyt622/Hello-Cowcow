package com.example.hellocowcow.data.rewards

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MooveRewardDecoderTest {

  @Test
  fun `decodes legacy CowCow reward amount`() {
    // Base64 decodes to 07 | 00005af3107a4000.
    // The first byte is the legacy encoded-length prefix; the remaining bytes equal 1e14.
    val result = MooveRewardDecoder.decodeClaimableAmount(
      "contract-prefix-BwBa8xB6QAA"
    )

    assertEquals(0, BigDecimal("0.0001").compareTo(result))
  }

  @Test
  fun `preserves all token decimals instead of rounding recovery liquidity`() {
    // Base64 decodes to 07 | 0462d53c8abac0 = 1234567890123456 atomic MOOVE.
    val result = MooveRewardDecoder.decodeClaimableAmount(
      "contract-prefix-BwRi1TyKusA"
    )

    assertEquals(0, BigDecimal("0.001234567890123456").compareTo(result))
  }

  @Test
  fun `short unexpected response fails with a domain error instead of substring crash`() {
    val error = assertThrows(IllegalArgumentException::class.java) {
      MooveRewardDecoder.decodeClaimableAmount("invalid")
    }

    assertEquals(
      "Unable to locate the MOOVE reward amount in contract data",
      error.message
    )
  }

  @Test
  fun `empty response is rejected explicitly`() {
    assertThrows(IllegalArgumentException::class.java) {
      MooveRewardDecoder.decodeClaimableAmount("")
    }
  }
}
