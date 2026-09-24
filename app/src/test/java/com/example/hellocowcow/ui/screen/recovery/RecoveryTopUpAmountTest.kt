package com.example.hellocowcow.ui.screen.recovery

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.models.RecoverySnapshot
import com.example.hellocowcow.domain.recovery.RecoveryPlanCalculator
import com.example.hellocowcow.domain.transactions.RecoveryTopUpTransactionFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Base64
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryTopUpAmountTest {
  private val atom = BigDecimal("0.000000000000000001")
  private val presets = listOf("0.5", "1", "2", "5").map(::BigDecimal)

  @Test
  fun `one percent of a full precision gap reproduces and fixes the rejection`() {
    // Synthetic full-precision example, not a hardcoded live wallet amount.
    val gap = BigDecimal("106000.123456789012345678")
    val oldAmount = gap.multiply(BigDecimal("1.01"))
    assertEquals("107060.12469135690246913478", oldAmount.toPlainString())
    val error = assertThrows(IllegalArgumentException::class.java) {
      RecoveryTopUpTransactionFactory.create(DomainAccount(address = "erd1sender"), oldAmount)
    }
    assertEquals("MOOVE amount supports at most 18 decimal places", error.message)

    val selected = calculateAvailableTopUp(snapshot(gap, BigDecimal("200000")), BigDecimal.ONE)
    assertAmount("107060.124691356902469135", selected)
    assertEquals(BigInteger("107060124691356902469135"), encodedAtoms(selected))
  }

  @Test
  fun `every preset rounds up by less than one atomic unit`() {
    val gap = BigDecimal("2000.123456789012345678")
    for (preset in presets) {
      val exact = gap.multiply(BigDecimal.ONE.add(preset.movePointLeft(2)))
      val desired = calculateDesiredTopUp(gap, preset)
      assertTrue(desired >= exact)
      assertTrue(desired.subtract(exact) < atom)
      assertEquals(desired.movePointRight(18).toBigIntegerExact(), encodedAtoms(desired))
    }
  }

  @Test
  fun `integer examples and zero percent retain their exact amounts`() {
    assertAmount("2020", calculateDesiredTopUp(BigDecimal("2000"), BigDecimal.ONE))
    val gap = BigDecimal("2000.123456789012345678")
    assertEquals(0, gap.compareTo(calculateDesiredTopUp(gap, BigDecimal.ZERO)))
  }

  @Test
  fun `wallet cap wins over rounding up the desired safety margin`() {
    val selected = calculateAvailableTopUp(
      snapshot(BigDecimal("2000"), BigDecimal("2012")), BigDecimal.ONE
    )
    assertAmount("2012", selected)
    assertEquals(BigInteger("2012000000000000000000"), encodedAtoms(selected))
  }

  @Test
  fun `wallet at exactly the gap remains usable without an extra atomic unit`() {
    val gap = BigDecimal("2000.123456789012345678")
    for (preset in presets) {
      val selected = calculateAvailableTopUp(snapshot(gap, gap), preset)
      assertEquals(0, gap.compareTo(selected))
      assertEquals(gap.movePointRight(18).toBigIntegerExact(), encodedAtoms(selected))
    }
  }

  @Test
  fun `wallet caps never round above a reported balance`() {
    val wallet = BigDecimal("2012.1234567890123456789")
    val selected = calculateAvailableTopUp(snapshot(BigDecimal("2000"), wallet), BigDecimal.ONE)
    assertAmount("2012.123456789012345678", selected)
    assertTrue(selected <= wallet)
    encodedAtoms(selected)
  }

  @Test
  fun `single atomic unit remains positive and respects a single atom wallet`() {
    for (preset in presets) {
      assertEquals(0, atom.multiply(BigDecimal("2")).compareTo(calculateDesiredTopUp(atom, preset)))
      val selected = calculateAvailableTopUp(snapshot(atom, atom), preset)
      assertEquals(BigInteger.ONE, encodedAtoms(selected))
    }
  }

  @Test
  fun `zero gap and empty wallet remain zero and cannot build transfers`() {
    for (position in listOf(snapshot(BigDecimal.ZERO, BigDecimal("100")), snapshot(BigDecimal.ONE, BigDecimal.ZERO))) {
      val selected = calculateAvailableTopUp(position, BigDecimal.ONE)
      assertAmount("0", selected)
      assertThrows(IllegalArgumentException::class.java) {
        RecoveryTopUpTransactionFactory.create(DomainAccount(address = "erd1sender"), selected)
      }
    }
  }

  @Test
  fun `an underfunded wallet is not made to cover the gap by rounding`() {
    val gap = BigDecimal("2000.123456789012345678")
    val wallet = gap.subtract(atom)
    val selected = calculateAvailableTopUp(snapshot(gap, wallet), BigDecimal.ONE)
    assertTrue(selected < gap)
    assertEquals(0, wallet.compareTo(selected))
  }

  @Test
  fun `canonical amount does not change guardian safeguards`() {
    val selected = calculateDesiredTopUp(BigDecimal("1.123456789012345678"), BigDecimal("0.5"))
    val tx = RecoveryTopUpTransactionFactory.create(
      DomainAccount(address = "erd1sender", nonce = 42, isGuarded = true, activeGuardianAddress = "erd1guardian"),
      selected
    )
    assertEquals(42, tx.nonce)
    assertEquals(2, tx.version)
    assertEquals(2, tx.options)
    assertEquals("erd1guardian", tx.guardian)
    assertEquals(CowCowConfig.ESDT_TRANSFER_GAS_LIMIT + CowCowConfig.GUARDED_TRANSACTION_GAS_OVERHEAD, tx.gasLimit)
  }

  @Test
  fun `many atomic amounts match an independent integer ceiling oracle`() {
    val random = Random(18L)
    val divisor = BigInteger("1000")
    val bumps = listOf(5, 10, 20, 50) // 0.5%, 1%, 2%, 5% in thousandths.
    repeat(100) {
      val gapAtoms = BigInteger(100, random).add(BigInteger.ONE)
      val gap = gapAtoms.toBigDecimal(18)
      for (index in presets.indices) {
        val numerator = gapAtoms.multiply(BigInteger.valueOf(1000L + bumps[index]))
        val expected = numerator.add(divisor.subtract(BigInteger.ONE)).divide(divisor)
        val desired = calculateDesiredTopUp(gap, presets[index])
        assertEquals(expected, desired.movePointRight(18).toBigIntegerExact())
        for (walletAtoms in listOf(gapAtoms.subtract(BigInteger.ONE), gapAtoms, expected, expected.add(BigInteger.ONE))) {
          val wallet = walletAtoms.toBigDecimal(18)
          val selected = calculateAvailableTopUp(snapshot(gap, wallet), presets[index])
          assertEquals(expected.min(walletAtoms), encodedAtoms(selected))
          assertTrue(selected <= wallet)
          assertTrue(selected.stripTrailingZeros().scale() <= 18)
          if (wallet >= gap) assertTrue(selected >= gap)
        }
      }
    }
  }

  private fun snapshot(gap: BigDecimal, wallet: BigDecimal): RecoverySnapshot =
    RecoveryPlanCalculator.create(gap, wallet, BigDecimal.ZERO)

  private fun assertAmount(expected: String, actual: BigDecimal) {
    assertEquals(0, BigDecimal(expected).compareTo(actual))
  }

  private fun encodedAtoms(amount: BigDecimal): BigInteger {
    val tx = RecoveryTopUpTransactionFactory.create(DomainAccount(address = "erd1sender"), amount)
    val payload = String(Base64.getDecoder().decode(tx.data), Charsets.UTF_8).split('@')
    assertEquals("ESDTTransfer", payload[0])
    assertEquals("4d4f4f56452d383735353339", payload[1])
    return BigInteger(payload[2], 16)
  }
}
