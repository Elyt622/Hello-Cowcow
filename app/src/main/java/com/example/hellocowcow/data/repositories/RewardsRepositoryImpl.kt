package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.network.api.MvxApi
import com.example.hellocowcow.data.retrofit.mvxApi.request.Reward
import com.example.hellocowcow.domain.repositories.RewardsRepository
import javax.inject.Inject

class RewardsRepositoryImpl @Inject constructor(
  private val mvxApi: MvxApi
) : RewardsRepository {

  override suspend fun getUserData(address: String): String {
    require(address.isNotBlank()) { "Wallet address is required to load rewards" }

    val response = mvxApi.queryContract(
      Reward(
        scAddress = CowCowConfig.REWARDS_CONTRACT,
        funcName = CowCowConfig.REWARDS_USER_DATA_FUNCTION,
        value = "0",
        args = arrayListOf(),
        caller = address
      )
    )

    return response.returnData.firstOrNull()
      ?: throw IllegalStateException("Rewards contract returned no data")
  }
}
