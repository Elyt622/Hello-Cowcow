package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.network.api.MvxApi
import com.example.hellocowcow.data.rewards.MooveRewardDecoder
import com.example.hellocowcow.domain.models.RecoverySnapshot
import com.example.hellocowcow.domain.recovery.RecoveryPlanCalculator
import com.example.hellocowcow.domain.repositories.RecoveryRepository
import com.example.hellocowcow.domain.repositories.RewardsRepository
import java.math.BigDecimal
import javax.inject.Inject
import retrofit2.HttpException

class RecoveryRepositoryImpl @Inject constructor(
  private val mvxApi: MvxApi,
  private val rewardsRepository: RewardsRepository
) : RecoveryRepository {

  override suspend fun getSnapshot(
    address: String,
    forceRefreshRewards: Boolean
  ): RecoverySnapshot {
    require(address.isNotBlank()) { "Wallet address is required for recovery diagnostics" }

    val claimableRewards = MooveRewardDecoder.decodeClaimableAmount(
      rewardsRepository.getUserData(address, forceRefresh = forceRefreshRewards)
    )
    val walletBalance = getMooveBalance(address)
    val contractBalance = getMooveBalance(CowCowConfig.REWARDS_CONTRACT)

    return RecoveryPlanCalculator.create(
      claimableRewards = claimableRewards,
      walletMooveBalance = walletBalance,
      contractMooveBalance = contractBalance
    )
  }

  private suspend fun getMooveBalance(address: String): BigDecimal {
    val token = try {
      mvxApi.getAccountToken(address, CowCowConfig.MOOVE_TOKEN_ID)
    } catch (error: HttpException) {
      if (error.code() == 404) return BigDecimal.ZERO
      throw error
    }

    val rawBalance = token.balance
      ?.takeIf { it.isNotBlank() }
      ?.toBigIntegerOrNull()
      ?: return BigDecimal.ZERO
    val decimals = token.decimals ?: MOOVE_DECIMALS

    return rawBalance.toBigDecimal(decimals).stripTrailingZeros()
  }

  private companion object {
    const val MOOVE_DECIMALS = 18
  }
}
