package com.example.hellocowcow.core.wallet

import com.example.hellocowcow.domain.models.MvxTransaction
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MvxSignTransactionResultParserTest {

  private val signature = "ab".repeat(64)
  private val guardianSignature = "cd".repeat(64)

  @Test
  fun `parses a regular xPortal signature result`() {
    val parsed = MvxSignTransactionResultParser.parse(
      mapOf("signature" to signature)
    ).getOrThrow()

    assertEquals(signature, parsed.signature)
    assertNull(parsed.guardian)
    assertNull(parsed.guardianSignature)
  }

  @Test
  fun `parses guardian fields returned by xPortal`() {
    val parsed = MvxSignTransactionResultParser.parse(
      mapOf(
        "signature" to signature,
        "guardian" to "erd1guardian",
        "guardianSignature" to guardianSignature,
        "options" to 2,
        "version" to 2
      )
    ).getOrThrow()

    assertEquals("erd1guardian", parsed.guardian)
    assertEquals(guardianSignature, parsed.guardianSignature)
    assertEquals(2, parsed.options)
    assertEquals(2, parsed.version)
  }

  @Test
  fun `regular signed transaction never serializes guardianSignature as null string`() {
    val parsed = MvxSignTransactionResultParser.parse(
      "{\"signature\":\"$signature\"}"
    ).getOrThrow()

    val signed = parsed.applyTo(
      MvxTransaction(
        nonce = 1,
        receiver = "erd1receiver",
        sender = "erd1sender"
      )
    )

    assertNull(signed.guardianSignature)
    val json = Gson().toJson(signed)
    assertFalse(json.contains("guardianSignature"))
    assertFalse(json.contains("\"null\""))
  }

  @Test
  fun `rejects malformed wallet signatures`() {
    val result = MvxSignTransactionResultParser.parse(
      mapOf("signature" to "not-a-signature")
    )

    assertTrue(result.isFailure)
  }
}
