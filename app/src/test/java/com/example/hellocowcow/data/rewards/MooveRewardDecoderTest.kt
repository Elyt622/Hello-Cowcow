package com.example.hellocowcow.data.rewards

import com.example.hellocowcow.data.recovery.CowCowDataOutFixture
import com.example.hellocowcow.data.recovery.CowCowDataOutFixture.Payment
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MooveRewardDecoderTest {
  @Test
  fun `real 104 CowCow response returns the identified MOOVE payment exactly`() {
    assertAmount("107243.50430364948", CowCowDataOutFixture.recorded("mainnet-104-cows"))
  }

  @Test
  fun `real empty position is zero despite nonzero global contract settings`() {
    assertAmount("0", CowCowDataOutFixture.recorded("mainnet-empty"))
  }

  @Test
  fun `selects MOOVE by token identifier in every reward list position`() {
    val moove = Payment("MOOVE-875539", BigInteger("106000123456789012345678"))
    val bigger = Payment("OTHER-abcdef", BigInteger.TEN.pow(60))
    for (payments in listOf(listOf(moove, bigger), listOf(bigger, moove))) {
      assertAmount("106000.123456789012345678", CowCowDataOutFixture.encode(payments = payments))
    }
  }

  @Test
  fun `no MOOVE payment is zero even when other token rewards are huge`() {
    assertAmount("0", CowCowDataOutFixture.encode(
      payments = listOf(Payment("OTHER-abcdef", BigInteger.TEN.pow(60)))
    ))
  }

  @Test
  fun `zero and single atomic MOOVE amounts keep full precision`() {
    for (atomic in listOf(BigInteger.ZERO, BigInteger.ONE)) {
      assertAmount(atomic.toBigDecimal(18).toPlainString(), CowCowDataOutFixture.encode(
        payments = listOf(Payment("MOOVE-875539", atomic))
      ))
    }
  }

  @Test
  fun `sums multiple fungible MOOVE payments only`() {
    assertAmount("3", CowCowDataOutFixture.encode(payments = listOf(
      Payment("MOOVE-875539", BigInteger.TEN.pow(18)),
      Payment("OTHER-abcdef", BigInteger.TEN.pow(60)),
      Payment("MOOVE-875539", BigInteger.valueOf(2).multiply(BigInteger.TEN.pow(18)))
    )))
  }

  @Test
  fun `unstaked lists and shares do not change the reward field`() {
    assertAmount("1", CowCowDataOutFixture.encode(
      staked = listOf(1L, 104L, 65536L),
      unstaked = listOf(999L to 1800000000L, 123L to 1800000100L),
      payments = listOf(Payment("MOOVE-875539", BigInteger.TEN.pow(18))),
      shares = BigInteger.TEN.pow(70)
    ))
  }

  @Test
  fun `nonzero MOOVE nonce is rejected rather than treated as fungible rewards`() {
    assertThrows(IllegalArgumentException::class.java) {
      MooveRewardDecoder.decodeClaimableAmount(CowCowDataOutFixture.encode(
        payments = listOf(Payment("MOOVE-875539", BigInteger.ONE, nonce = 1))
      ))
    }
  }

  @Test
  fun `every truncated prefix of a real payload fails closed`() {
    val bytes = Base64.getDecoder().decode(CowCowDataOutFixture.recorded("mainnet-104-cows"))
    for (size in bytes.indices) {
      assertThrows(IllegalArgumentException::class.java) {
        MooveRewardDecoder.decodeClaimableAmount(
          Base64.getEncoder().encodeToString(bytes.copyOf(size))
        )
      }
    }
  }

  @Test
  fun `unknown trailing fields fail closed`() {
    val bytes = Base64.getDecoder().decode(CowCowDataOutFixture.recorded("mainnet-empty"))
    assertThrows(IllegalArgumentException::class.java) {
      MooveRewardDecoder.decodeClaimableAmount(Base64.getEncoder().encodeToString(bytes + 0.toByte()))
    }
  }

  @Test
  fun `legacy fragments malformed Base64 and empty input are rejected`() {
    for (input in listOf("", "invalid", "!!!!", "contract-prefix-BwBa8xB6QAA")) {
      assertThrows(IllegalArgumentException::class.java) {
        MooveRewardDecoder.decodeClaimableAmount(input)
      }
    }
  }

  private fun assertAmount(expected: String, encoded: String) {
    assertEquals(0, BigDecimal(expected).compareTo(MooveRewardDecoder.decodeClaimableAmount(encoded)))
  }
}
