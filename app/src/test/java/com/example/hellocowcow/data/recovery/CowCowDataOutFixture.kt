package com.example.hellocowcow.data.recovery

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.util.Base64

/** Synthetic fixtures use the recovered DataOut ABI, not guessed Base64 substrings. */
object CowCowDataOutFixture {
  data class Payment(
    val token: String,
    val amount: BigInteger,
    val nonce: Long = 0
  )

  fun encode(
    staked: List<Long> = emptyList(),
    unstaked: List<Pair<Long, Long>> = emptyList(),
    payments: List<Payment> = emptyList(),
    metadata: BigInteger = BigInteger("999999999999999999999999999999999999999999"),
    shares: BigInteger = BigInteger("31200")
  ): String {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { out ->
      out.writeInt(staked.size)
      staked.forEach(out::writeLong)
      out.writeInt(unstaked.size)
      unstaked.forEach { (nonce, unlockTime) ->
        out.writeLong(nonce)
        out.writeLong(unlockTime)
      }
      repeat(5) { out.writeBigUint(metadata) }
      out.writeLong(604800L)
      out.writeInt(payments.size)
      payments.forEach { payment ->
        val token = payment.token.toByteArray(Charsets.US_ASCII)
        out.writeInt(token.size)
        out.write(token)
        out.writeLong(payment.nonce)
        out.writeBigUint(payment.amount)
      }
      out.writeBigUint(shares)
    }
    return Base64.getEncoder().encodeToString(bytes.toByteArray())
  }

  private fun DataOutputStream.writeBigUint(value: BigInteger) {
    require(value.signum() >= 0)
    val bytes = if (value == BigInteger.ZERO) byteArrayOf() else {
      value.toByteArray().let {
        if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it
      }
    }
    writeInt(bytes.size)
    write(bytes)
  }

  fun recorded(name: String): String = requireNotNull(
    CowCowDataOutFixture::class.java.getResourceAsStream("/cowcow/$name.base64")
  ) { "Missing mainnet fixture $name" }.bufferedReader().use { it.readText().trim() }
}
