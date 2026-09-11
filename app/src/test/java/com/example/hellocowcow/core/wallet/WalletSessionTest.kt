package com.example.hellocowcow.core.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletSessionTest {

  @Test
  fun `extracts MultiversX account from approved session`() {
    val session = walletSession(
      topic = "topic-123",
      accounts = listOf(
        "eip155:1:0xabc",
        "mvx:1:erd1cowcow"
      )
    )

    assertEquals("erd1cowcow", session?.address)
    assertEquals("topic-123", session?.topic)
  }

  @Test
  fun `ignores approved sessions without a MultiversX account`() {
    assertNull(
      walletSession(
        topic = "topic-123",
        accounts = listOf("eip155:1:0xabc")
      )
    )
  }
}
