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
    // The first byte is the tail of the legacy nested-length prefix; this fixture
    // intentionally is not a complete getAllDataForUser Base64 payload.
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
  fun `decodes large reward before trailing zero metadata for 104 CowCows`() {
    val rewardAtomic = BigDecimal("106000.123456789012345678")
      .movePointRight(18)
      .toBigIntegerExact()

    val stakePrefix = buildList<Byte> {
      add(0x00)
      add(0x68) // 104 CowCows
      repeat(104) { index ->
        add(((index + 1) ushr 8).toByte())
        add(((index + 1) and 0xff).toByte())
      }
    }.toByteArray()

    val bytes = stakePrefix +
        encodeNestedBigUint(rewardAtomic) +
        byteArrayOf(0x00, 0x00, 0x00, 0x00)

    val encoded = Base64.getEncoder().encodeToString(bytes)
    val result = MooveRewardDecoder.decodeClaimableAmount(encoded)

    assertEquals(
      0,
      BigDecimal("106000.123456789012345678").compareTo(result)
    )
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
  fun `ambiguous equally-sized BigUints fail closed instead of guessing`() {
    val first = BigInteger("12345678901234567890123")
    val second = BigInteger("22345678901234567890123")
    val bytes = byteArrayOf(0x00, 0x01) +
        encodeNestedBigUint(first) +
        encodeNestedBigUint(second)

    val encoded = Base64.getEncoder().encodeToString(bytes)

    assertThrows(IllegalArgumentException::class.java) {
      MooveRewardDecoder.decodeClaimableAmount(encoded)
    }
  }

  @Test
  fun `short unexpected response fails with a domain error instead of substring crash`() {
    val error = assertThrows(IllegalArgumentException::class.java) {
      MooveRewardDecoder.decodeClaimableAmount("invalid")
    }

    assertEquals(
      "Unable to locate the MOOVE reward BigUint in contract data",
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
  ): String = Base64.getEncoder().encodeToString(
    prefix + encodeNestedBigUint(atomicAmount)
  )

  private fun encodeNestedBigUint(
    atomicAmount: BigInteger
  ): ByteArray {
    val amountBytes = atomicAmount.toByteArray().let { bytes ->
      if (bytes.size > 1 && bytes.first() == 0.toByte()) {
        bytes.copyOfRange(1, bytes.size)
      } else {
        bytes
      }
    }
    val length = amountBytes.size
    return byteArrayOf(
      ((length ushr 24) and 0xff).toByte(),
      ((length ushr 16) and 0xff).toByte(),
      ((length ushr 8) and 0xff).toByte(),
      (length and 0xff).toByte()
    ) + amountBytes
  }
}
