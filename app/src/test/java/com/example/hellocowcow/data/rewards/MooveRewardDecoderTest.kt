package com.example.hellocowcow.data.rewards

import java.math.BigDecimal
import java.math.BigInteger
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MooveRewardDecoderTest {

  @Test
  fun `decodes legacy CowCow reward amount`() {
    // Base64 decodes to 07 | 00005af3107a4000.
    // The first byte is the tail of the legacy nested-length prefix.
    val result = MooveRewardDecoder.decodeClaimableAmount(
      "contract-prefix-BwBa8xB6QAA"
    )

    assertEquals(0, BigDecimal("0.0001").compareTo(result))
  }

  @Test
  fun `preserves all token decimals instead of rounding recovery liquidity`() {
    val atomicAmount = BigInteger("1234567890123456")
    val encoded = encodeNestedReward(
      prefix = byteArrayOf(0x00, 0x04, 0x07, 0x88.toByte()),
      atomicAmount = atomicAmount
    )

    val result = MooveRewardDecoder.decodeClaimableAmount(encoded)

    assertEquals(0, BigDecimal("0.001234567890123456").compareTo(result))
  }

  @Test
  fun `nested zero reward decodes to zero after claimRewards`() {
    val bytes = byteArrayOf(
      0x00, 0x00, // synthetic staked-Cow count
      0x00, 0x00, 0x00, 0x00 // nested BigUint zero length
    )
    val encoded = Base64.getEncoder().encodeToString(bytes)

    val result = MooveRewardDecoder.decodeClaimableAmount(encoded)

    assertEquals(0, BigDecimal.ZERO.compareTo(result))
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

  private fun encodeNestedReward(
    prefix: ByteArray,
    atomicAmount: BigInteger
  ): String {
    val amountBytes = atomicAmount.toByteArray().let { bytes ->
      if (bytes.size > 1 && bytes.first() == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
    }
    val length = amountBytes.size
    val nested = byteArrayOf(
      ((length ushr 24) and 0xff).toByte(),
      ((length ushr 16) and 0xff).toByte(),
      ((length ushr 8) and 0xff).toByte(),
      (length and 0xff).toByte()
    ) + amountBytes

    return Base64.getEncoder().encodeToString(prefix + nested)
  }
}
