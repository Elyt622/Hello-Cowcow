package com.example.hellocowcow.data.recovery

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CowCowUserDataDecoderTest {

  @Test
  fun `skips leading zero fields before CowCow stake count`() {
    val bytes = byteArrayOf(
      0x00, 0x00, // unrelated leading field from getAllDataForUser
      0x00, 0x04, // staked Cow count
      0x07, 0x88.toByte(),
      0x0e, 0xb2.toByte(),
      0x26, 0x7e,
      0x0f, 0xde.toByte(),
      0x01, 0x02, 0x03
    )
    val encoded = Base64.getEncoder().encodeToString(bytes)

    assertEquals(
      listOf("0788", "0eb2", "267e", "0fde"),
      CowCowUserDataDecoder.decodeStakedCowNonces(encoded)
    )
  }

  @Test
  fun `preserves two-byte CowCow nonces used by verified exit history`() {
    val bytes = byteArrayOf(
      0x00, 0x04,
      0x07, 0x88.toByte(),
      0x0e, 0xb2.toByte(),
      0x26, 0x7e,
      0x0f, 0xde.toByte(),
      0x01, 0x02, 0x03
    )
    val encoded = Base64.getEncoder().encodeToString(bytes)

    assertEquals(
      listOf("0788", "0eb2", "267e", "0fde"),
      CowCowUserDataDecoder.decodeStakedCowNonces(encoded)
    )
  }

  @Test
  fun `normalizes legacy fixed-width low nonce to minimal hex bytes`() {
    val bytes = byteArrayOf(
      0x00, 0x02,
      0x00, 0x01,
      0x00, 0xff.toByte()
    )
    val encoded = Base64.getEncoder().encodeToString(bytes)

    assertEquals(
      listOf("01", "ff"),
      CowCowUserDataDecoder.decodeStakedCowNonces(encoded)
    )
  }

  @Test
  fun `all-zero staking data returns empty list`() {
    val encoded = Base64.getEncoder().encodeToString(
      byteArrayOf(0x00, 0x00, 0x00, 0x00)
    )

    assertEquals(emptyList<String>(), CowCowUserDataDecoder.decodeStakedCowNonces(encoded))
  }

  @Test
  fun `truncated stake data is rejected`() {
    val encoded = Base64.getEncoder().encodeToString(
      byteArrayOf(0x00, 0x02, 0x07, 0x88.toByte())
    )

    assertThrows(IllegalArgumentException::class.java) {
      CowCowUserDataDecoder.decodeStakedCowNonces(encoded)
    }
  }
}
