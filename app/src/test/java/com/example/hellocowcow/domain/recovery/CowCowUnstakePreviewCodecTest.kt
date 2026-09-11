package com.example.hellocowcow.domain.recovery

import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test

class CowCowUnstakePreviewCodecTest {

  @Test
  fun `reproduces verified four CowCow unstake payload exactly`() {
    val encoded = CowCowUnstakePreviewCodec.encode(
      listOf("0788", "0eb2", "267e", "0fde")
    )

    assertEquals(
      "unstake@0788@0eb2@267e@0fde",
      encoded.payload
    )
    assertEquals(
      encoded.payload,
      String(Base64.getDecoder().decode(encoded.dataBase64), StandardCharsets.UTF_8)
    )
  }

  @Test
  fun `reproduces verified 72 CowCow batch payload`() {
    val nonces = listOf(
      "0c71", "114f", "1261", "1cd1", "1ce8", "15d8", "2681", "075c",
      "1d45", "1bea", "1d2b", "0181", "1140", "23c0", "1126", "202e",
      "12c0", "0433", "0a10", "10eb", "1ecc", "2224", "25f8", "18b6",
      "18b5", "1c61", "22ae", "21b9", "2135", "0a1c", "1d41", "1562",
      "0282", "02d0", "15ff", "1f2a", "2319", "24a8", "0d22", "113d",
      "0a46", "14f5", "1292", "04f9", "0e93", "1827", "1f0c", "1f2e",
      "263f", "26e7", "0b6b", "25f1", "19a5", "0784", "11fd", "2027",
      "108a", "0287", "02c5", "0758", "0826", "0c1f", "0f2b", "1eed",
      "1ef6", "0643", "0c34", "1aa8", "1eca", "0c0e", "0738", "0372"
    )

    val encoded = CowCowUnstakePreviewCodec.encode(nonces)

    assertEquals(72, encoded.nonces.size)
    assertEquals("unstake@${nonces.joinToString("@")}", encoded.payload)
  }

  @Test
  fun `normalizes low CowCow nonce as whole hex bytes`() {
    val encoded = CowCowUnstakePreviewCodec.encode(listOf("0001", "00ff", "0788"))

    assertEquals(listOf("01", "ff", "0788"), encoded.nonces)
    assertEquals("unstake@01@ff@0788", encoded.payload)
  }
}
