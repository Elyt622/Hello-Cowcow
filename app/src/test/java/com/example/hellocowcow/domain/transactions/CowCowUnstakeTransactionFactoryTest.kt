package com.example.hellocowcow.domain.transactions

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.domain.models.DomainAccount
import com.google.gson.Gson
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CowCowUnstakeTransactionFactoryTest {

  @Test
  fun `unguarded unstake matches verified CowCow payload`() {
    val transaction = CowCowUnstakeTransactionFactory.create(
      account = DomainAccount(
        address = "erd1sender",
        nonce = 42,
        isGuarded = false
      ),
      cowNonces = listOf("0788", "0eb2", "267e", "0fde")
    )

    assertEquals(42, transaction.nonce)
    assertEquals("0", transaction.value)
    assertEquals(CowCowConfig.REWARDS_CONTRACT, transaction.receiver)
    assertEquals("erd1sender", transaction.sender)
    assertEquals(CowCowConfig.MIN_GAS_PRICE, transaction.gasPrice)
    assertEquals(CowCowConfig.UNSTAKE_GAS_LIMIT, transaction.gasLimit)
    assertEquals(CowCowConfig.MAINNET_CHAIN_ID, transaction.chainID)
    assertEquals(
      "unstake@0788@0eb2@267e@0fde",
      String(Base64.getDecoder().decode(transaction.data), StandardCharsets.UTF_8)
    )
    assertEquals(1, transaction.version)
    assertNull(transaction.options)
    assertNull(transaction.guardian)
    assertNull(transaction.guardianSignature)

    val json = Gson().toJson(transaction)
    assertFalse(json.contains("\"options\""))
    assertFalse(json.contains("\"guardian\""))
    assertFalse(json.contains("\"guardianSignature\""))
    assertFalse(json.contains("\"signature\""))
  }

  @Test
  fun `unstake normalizes legacy fixed-width CowCow nonces`() {
    val transaction = CowCowUnstakeTransactionFactory.create(
      account = DomainAccount(address = "erd1sender"),
      cowNonces = listOf("0001", "0788")
    )

    assertEquals(
      "unstake@01@0788",
      String(Base64.getDecoder().decode(transaction.data), StandardCharsets.UTF_8)
    )
  }

  @Test
  fun `guarded unstake adds guardian fields and gas overhead`() {
    val transaction = CowCowUnstakeTransactionFactory.create(
      account = DomainAccount(
        address = "erd1sender",
        nonce = 7,
        activeGuardianAddress = "erd1guardian",
        isGuarded = true
      ),
      cowNonces = listOf("0788")
    )

    assertEquals(
      CowCowConfig.UNSTAKE_GAS_LIMIT + CowCowConfig.GUARDED_TRANSACTION_GAS_OVERHEAD,
      transaction.gasLimit
    )
    assertEquals(2, transaction.version)
    assertEquals(2, transaction.options)
    assertEquals("erd1guardian", transaction.guardian)
  }

  @Test
  fun `guarded unstake requires active guardian address`() {
    assertThrows(IllegalArgumentException::class.java) {
      CowCowUnstakeTransactionFactory.create(
        account = DomainAccount(
          address = "erd1sender",
          isGuarded = true,
          activeGuardianAddress = ""
        ),
        cowNonces = listOf("0788")
      )
    }
  }

  @Test
  fun `unstake requires at least one CowCow`() {
    assertThrows(IllegalArgumentException::class.java) {
      CowCowUnstakeTransactionFactory.create(
        account = DomainAccount(address = "erd1sender"),
        cowNonces = emptyList()
      )
    }
  }
}
