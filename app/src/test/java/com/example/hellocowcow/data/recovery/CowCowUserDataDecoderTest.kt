package com.example.hellocowcow.data.recovery

import com.example.hellocowcow.data.recovery.CowCowDataOutFixture.Payment
import java.math.BigInteger
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CowCowUserDataDecoderTest {
  @Test
  fun `real mainnet response decodes exactly 104 u64 nonces`() {
    val data = CowCowUserDataDecoder.decode(CowCowDataOutFixture.recorded("mainnet-104-cows"))
    assertEquals(104, data.stakedCowNonces.size)
    assertEquals("02b8", data.stakedCowNonces.first())
    assertEquals("16e5", data.stakedCowNonces.last())
    assertEquals(BigInteger("31200"), data.shares)
    assertEquals("MOOVE-875539", data.rewards.single().tokenIdentifier)
    assertEquals(BigInteger("107243504303649480000000"), data.rewards.single().amount)
  }

  @Test
  fun `empty stake list stays empty when user has rewards and shares`() {
    val encoded = CowCowDataOutFixture.encode(
      payments = listOf(Payment("MOOVE-875539", BigInteger.TEN.pow(23)))
    )
    assertEquals(emptyList<String>(), CowCowUserDataDecoder.decodeStakedCowNonces(encoded))
  }

  @Test
  fun `u64 nonce widths are preserved including leading and internal zero bytes`() {
    val encoded = CowCowDataOutFixture.encode(staked = listOf(1L, 255L, 65536L, 4294967296L))
    assertEquals(listOf("01", "ff", "010000", "0100000000"),
      CowCowUserDataDecoder.decodeStakedCowNonces(encoded))
  }

  @Test
  fun `unstaked entries do not become staked nonces`() {
    val encoded = CowCowDataOutFixture.encode(unstaked = listOf(123L to 1800000000L))
    assertEquals(emptyList<String>(), CowCowUserDataDecoder.decodeStakedCowNonces(encoded))
  }

  @Test
  fun `zero and duplicate staked nonces are rejected`() {
    for (nonces in listOf(listOf(0L), listOf(1L, 1L))) {
      assertThrows(IllegalArgumentException::class.java) {
        CowCowUserDataDecoder.decodeStakedCowNonces(CowCowDataOutFixture.encode(staked = nonces))
      }
    }
  }

  @Test
  fun `impossible list size is rejected without allocating from untrusted input`() {
    val bytes = Base64.getDecoder().decode(CowCowDataOutFixture.recorded("mainnet-empty"))
    for (value in listOf(0xff.toByte(), 0x7f.toByte())) {
      bytes[0] = value
      assertThrows(IllegalArgumentException::class.java) {
        CowCowUserDataDecoder.decode(Base64.getEncoder().encodeToString(bytes))
      }
    }
  }

  @Test
  fun `incomplete all-zero data is not a valid empty DataOut`() {
    assertThrows(IllegalArgumentException::class.java) {
      CowCowUserDataDecoder.decode(Base64.getEncoder().encodeToString(ByteArray(4)))
    }
  }
}
