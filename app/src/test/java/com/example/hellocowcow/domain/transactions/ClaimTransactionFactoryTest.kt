package com.example.hellocowcow.domain.transactions

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.domain.models.DomainAccount
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ClaimTransactionFactoryTest {

  @Test
  fun `unguarded claim omits guardian fields`() {
    val transaction = ClaimTransactionFactory.create(
      DomainAccount(
        address = "erd1sender",
        nonce = 42,
        isGuarded = false
      )
    )

    assertEquals(42, transaction.nonce)
    assertEquals(CowCowConfig.CLAIM_REWARDS_GAS_LIMIT, transaction.gasLimit)
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
  fun `guarded claim adds guardian fields and gas overhead`() {
    val transaction = ClaimTransactionFactory.create(
      DomainAccount(
        address = "erd1sender",
        nonce = 7,
        activeGuardianAddress = "erd1guardian",
        isGuarded = true
      )
    )

    assertEquals(
      CowCowConfig.CLAIM_REWARDS_GAS_LIMIT + CowCowConfig.GUARDED_TRANSACTION_GAS_OVERHEAD,
      transaction.gasLimit
    )
    assertEquals(2, transaction.version)
    assertEquals(2, transaction.options)
    assertEquals("erd1guardian", transaction.guardian)
  }

  @Test
  fun `guarded claim requires active guardian address`() {
    assertThrows(IllegalArgumentException::class.java) {
      ClaimTransactionFactory.create(
        DomainAccount(
          address = "erd1sender",
          isGuarded = true,
          activeGuardianAddress = ""
        )
      )
    }
  }
}
