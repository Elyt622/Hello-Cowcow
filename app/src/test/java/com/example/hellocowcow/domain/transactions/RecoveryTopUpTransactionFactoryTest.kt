package com.example.hellocowcow.domain.transactions

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.domain.models.DomainAccount
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecoveryTopUpTransactionFactoryTest {

  @Test
  fun `creates protocol ESDT transfer for MOOVE top up`() {
    val account = DomainAccount(
      address = "erd1sender",
      nonce = 42
    )

    val tx = RecoveryTopUpTransactionFactory.create(
      account = account,
      amountMoove = BigDecimal("1.5")
    )

    assertEquals(42, tx.nonce)
    assertEquals("0", tx.value)
    assertEquals(CowCowConfig.REWARDS_CONTRACT, tx.receiver)
    assertEquals("erd1sender", tx.sender)
    assertEquals(CowCowConfig.ESDT_TRANSFER_GAS_LIMIT, tx.gasLimit)
    assertEquals(1, tx.version)
    assertNull(tx.guardian)
    assertNull(tx.options)

    val decodedData = String(
      Base64.getDecoder().decode(tx.data),
      StandardCharsets.UTF_8
    )
    assertEquals(
      "ESDTTransfer@4d4f4f56452d383735353339@14d1120d7b160000",
      decodedData
    )
  }

  @Test
  fun `pads an odd-length atomic amount to a whole hex byte`() {
    val tx = RecoveryTopUpTransactionFactory.create(
      account = DomainAccount(address = "erd1sender"),
      amountMoove = BigDecimal("0.000000000000000010")
    )

    val decodedData = String(
      Base64.getDecoder().decode(tx.data),
      StandardCharsets.UTF_8
    )

    // 10 atomic MOOVE units = byte 0x0a, matching SDK Buffer -> hex serialization.
    assertEquals(
      "ESDTTransfer@4d4f4f56452d383735353339@0a",
      decodedData
    )
  }

  @Test
  fun `adds guarded transaction metadata without changing transfer payload`() {
    val account = DomainAccount(
      address = "erd1sender",
      nonce = 7,
      activeGuardianAddress = "erd1guardian",
      isGuarded = true
    )

    val tx = RecoveryTopUpTransactionFactory.create(
      account = account,
      amountMoove = BigDecimal("10")
    )

    assertEquals(2, tx.version)
    assertEquals(2, tx.options)
    assertEquals("erd1guardian", tx.guardian)
    assertEquals(
      CowCowConfig.ESDT_TRANSFER_GAS_LIMIT + CowCowConfig.GUARDED_TRANSACTION_GAS_OVERHEAD,
      tx.gasLimit
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun `rejects fractional atomic units`() {
    RecoveryTopUpTransactionFactory.create(
      account = DomainAccount(address = "erd1sender"),
      amountMoove = BigDecimal("0.0000000000000000001")
    )
  }
}
