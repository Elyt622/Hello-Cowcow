package com.example.hellocowcow.core.wallet

import com.google.gson.Gson
import com.google.gson.JsonObject

data class MvxSignTransactionResult(
  val signature: String,
  val guardian: String? = null,
  val guardianSignature: String? = null,
  val options: Int? = null,
  val version: Int? = null
)

object MvxSignTransactionResultParser {

  private val gson = Gson()
  private val signaturePattern = Regex("^[0-9a-fA-F]{128}$")

  fun parse(payload: Any?): Result<MvxSignTransactionResult> = runCatching {
    val jsonObject = payload.toJsonObject()
    val signature = jsonObject.stringOrNull("signature")
      ?: error("Wallet response does not contain a transaction signature")

    validateSignature("signature", signature)

    val guardianSignature = jsonObject.stringOrNull("guardianSignature")
    guardianSignature?.let { validateSignature("guardianSignature", it) }

    MvxSignTransactionResult(
      signature = signature,
      guardian = jsonObject.stringOrNull("guardian"),
      guardianSignature = guardianSignature,
      options = jsonObject.intOrNull("options"),
      version = jsonObject.intOrNull("version")
    )
  }

  private fun Any?.toJsonObject(): JsonObject {
    requireNotNull(this) { "Wallet returned an empty transaction result" }

    return if (this is String) {
      gson.fromJson(this, JsonObject::class.java)
        ?: error("Wallet transaction result must be a JSON object")
    } else {
      val element = gson.toJsonTree(this)
      require(element.isJsonObject) {
        "Wallet transaction result must be a JSON object"
      }
      element.asJsonObject
    }
  }

  private fun JsonObject.stringOrNull(name: String): String? =
    get(name)
      ?.takeUnless { it.isJsonNull }
      ?.asString
      ?.takeIf { it.isNotBlank() }

  private fun JsonObject.intOrNull(name: String): Int? =
    get(name)
      ?.takeUnless { it.isJsonNull }
      ?.asInt

  private fun validateSignature(name: String, signature: String) {
    require(signaturePattern.matches(signature)) {
      "$name must be a 64-byte hex-encoded Ed25519 signature"
    }
  }
}
