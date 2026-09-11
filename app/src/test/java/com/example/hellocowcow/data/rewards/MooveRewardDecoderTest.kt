package com.example.hellocowcow.data.rewards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class MooveRewardDecoderTest {

  @Test
  fun `decodes legacy CowCow reward amount`() {
    // Base64 decodes to 07 | 00005af3107a4000.
    // The first byte is the legacy encoded-length prefix; the remaining bytes equal 1e14.
    val result = MooveRewardDecoder.decodeClaimableAmount(
      "contract-prefix-BwBa8xB6QAA"
    )

    assertEquals(BigDecimal("0.0001"), result)
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
