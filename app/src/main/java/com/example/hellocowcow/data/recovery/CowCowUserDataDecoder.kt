package com.example.hellocowcow.data.recovery

import com.example.hellocowcow.domain.recovery.CowCowNonceCodec
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Reads the complete DataOut struct from the original CowCow staking ABI.
 * Field order and real mainnet fixtures are recorded in docs/COWCOW_REWARD_DECODING.md.
 * Never search for integer-shaped bytes: list counts, u64 values and BigUints overlap.
 */
object CowCowUserDataDecoder {

  data class RewardPayment(
    val tokenIdentifier: String,
    val tokenNonce: BigInteger,
    val amount: BigInteger
  )

  data class UserData(
    val stakedCowNonces: List<String>,
    val rewards: List<RewardPayment>,
    val shares: BigInteger
  )

  fun decodeStakedCowNonces(returnData: String): List<String> =
    decode(returnData).stakedCowNonces

  fun decode(returnData: String): UserData {
    require(returnData.isNotBlank()) { "CowCow contract returned empty user data" }
    require(returnData.length <= MAX_BASE64_LENGTH) { "CowCow user data is too large" }
    val bytes = try {
      Base64.getDecoder().decode(returnData)
    } catch (error: IllegalArgumentException) {
      throw IllegalArgumentException("CowCow user data is not valid Base64", error)
    }
    val reader = Reader(bytes)

    // DataOut.staked_nfts: List<u64>. Every nonce occupies exactly eight bytes.
    val staked = List(reader.count("staked_nfts", 8)) {
      val hex = reader.bytes(8, "staked_nfts.nonce").joinToString("") {
        "%02x".format(it.toInt() and 0xff)
      }
      CowCowNonceCodec.normalize(hex)
    }
    require(staked.distinct().size == staked.size) { "Duplicate staked CowCow nonce" }

    // DataOut.unstaked_nfts: List<UnstakedNft { nonce: u64, unlock_time: u64 }>.
    repeat(reader.count("unstaked_nfts", 16)) {
      reader.bytes(16, "unstaked_nfts.item")
    }

    // These are global configuration values, NOT rewards belonging to the user.
    reader.bigUint("total_value_locked")
    reader.bigUint("reward_per_block")
    reader.bigUint("one_share")
    reader.bigUint("min_stake_amount")
    reader.bigUint("min_reward_amount")
    reader.bytes(8, "unlock_period")

    // The reward list carries the token identifier, nonce and amount for each payment.
    val rewards = List(reader.count("rewards_accumulated_for_user", 17)) {
      val tokenBytes = reader.nestedBytes("reward.token_identifier")
      require(tokenBytes.isNotEmpty() && tokenBytes.all { (it.toInt() and 0xff) in 33..126 }) {
        "Invalid CowCow reward token identifier"
      }
      RewardPayment(
        tokenIdentifier = String(tokenBytes, StandardCharsets.US_ASCII),
        tokenNonce = BigInteger(1, reader.bytes(8, "reward.token_nonce")),
        amount = reader.bigUint("reward.amount")
      )
    }
    val shares = reader.bigUint("shares")
    reader.requireEnd()
    return UserData(stakedCowNonces = staked, rewards = rewards, shares = shares)
  }

  private class Reader(private val data: ByteArray) {
    private var position = 0
    private val remaining: Int get() = data.size - position

    fun bytes(length: Int, field: String): ByteArray {
      require(length >= 0 && length <= remaining) {
        "Truncated CowCow user data at $field"
      }
      val result = data.copyOfRange(position, position + length)
      position += length
      return result
    }

    private fun length(field: String): Int {
      val encoded = bytes(4, "$field.length")
      val length = encoded.fold(0L) { value, byte ->
        (value shl 8) or (byte.toLong() and 0xffL)
      }
      require(length <= Int.MAX_VALUE) { "Invalid CowCow user data length at $field" }
      return length.toInt()
    }

    fun count(field: String, minimumItemSize: Int): Int {
      val count = length(field)
      require(count <= remaining / minimumItemSize) {
        "Truncated CowCow user data list at $field"
      }
      return count
    }

    fun nestedBytes(field: String): ByteArray = bytes(length(field), field)

    fun bigUint(field: String): BigInteger {
      val value = nestedBytes(field)
      return if (value.isEmpty()) BigInteger.ZERO else BigInteger(1, value)
    }

    fun requireEnd() {
      require(remaining == 0) { "Unexpected trailing CowCow user data; ABI may have changed" }
    }
  }

  private const val MAX_BASE64_LENGTH = 1_048_576
}
