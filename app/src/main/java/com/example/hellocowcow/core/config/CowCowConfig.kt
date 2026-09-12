package com.example.hellocowcow.core.config

object CowCowConfig {
  const val COLLECTION_ID = "COW-cd463d"
  const val TICKET_COLLECTION_ID = "TICKET-231cd2"
  const val MOOVE_TOKEN_ID = "MOOVE-875539"
  const val REWARDS_CONTRACT = "erd1qqqqqqqqqqqqqpgqqgzzsl0re9e3u0t3mhv3jwg6zu63zssd7yqs3uu9jk"
  const val REWARDS_USER_DATA_FUNCTION = "getAllDataForUser"
  const val UNSTAKE_FUNCTION = "unstake"
  const val FINAL_CLAIM_FUNCTION = "claim"

  const val MAINNET_CHAIN_ID = "1"
  const val MAINNET_CAIP_CHAIN_ID = "mvx:1"
  const val MIN_GAS_PRICE = 1_000_000_000L
  const val CLAIM_REWARDS_GAS_LIMIT = 30_000_000L

  // 600M is a verified historical CowCow unstake ceiling, but using that full
  // ceiling as the signed transaction limit can be rejected by current network
  // gas-per-transaction/block checks (and a guarded account adds extra gas on top).
  // 250M remains comfortably above the observed 72-Cow execution (~111M) while
  // leaving room for guarded-account overhead. Live /transaction/cost validation
  // still runs immediately before xPortal signing and fails closed if this is too low.
  const val UNSTAKE_GAS_LIMIT = 250_000_000L

  const val FINAL_CLAIM_GAS_LIMIT = 270_000_000L
  const val ESDT_TRANSFER_GAS_LIMIT = 500_000L
  const val GUARDED_TRANSACTION_GAS_OVERHEAD = 50_000L
  const val CLAIM_REWARDS_DATA = "Y2xhaW1SZXdhcmRz"
  const val MOOVE_DECIMALS = 18
}
