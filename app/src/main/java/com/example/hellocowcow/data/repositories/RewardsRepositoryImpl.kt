package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.network.api.MvxApi
import com.example.hellocowcow.data.retrofit.mvxApi.request.Reward
import com.example.hellocowcow.domain.repositories.RewardsRepository
import javax.inject.Inject

class RewardsRepositoryImpl @Inject constructor(
  private val mvxApi: MvxApi
) : RewardsRepository {

  @Volatile
  private var cachedAddress: String? = null

  @Volatile
  private var cachedValue: String? = null

  @Volatile
  private var cachedAtMillis: Long = 0L

  override suspend fun getUserData(address: String): String {
    require(address.isNotBlank()) { "Wallet address is required to load rewards" }

    val now = System.currentTimeMillis()
    val cached = cachedValue
    if (
      cached != null &&
      cachedAddress == address &&
      now - cachedAtMillis <= CACHE_TTL_MILLIS
    ) {
      return cached
    }

    val response = mvxApi.queryContract(
      Reward(
        scAddress = CowCowConfig.REWARDS_CONTRACT,
        funcName = CowCowConfig.REWARDS_USER_DATA_FUNCTION,
        value = "0",
        args = arrayListOf(),
        caller = address
      )
    )

    val value = response.returnData.firstOrNull()
      ?: throw IllegalStateException("Rewards contract returned no data")

    cachedAddress = address
    cachedValue = value
    cachedAtMillis = System.currentTimeMillis()

    return value
  }

  private companion object {
    const val CACHE_TTL_MILLIS = 5_000L
  }
}
