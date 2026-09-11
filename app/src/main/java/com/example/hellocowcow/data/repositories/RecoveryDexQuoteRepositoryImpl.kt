package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.network.api.XExchangeGraphQlRequest
import com.example.hellocowcow.data.network.api.XExchangeQuoteApi
import com.example.hellocowcow.data.network.api.XExchangeSwapRoute
import com.example.hellocowcow.data.network.api.XExchangeSwapVariables
import com.example.hellocowcow.domain.recovery.RecoveryDexQuote
import com.example.hellocowcow.domain.repositories.RecoveryDexQuoteRepository
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class RecoveryDexQuoteRepositoryImpl @Inject constructor(
  private val api: XExchangeQuoteApi
) : RecoveryDexQuoteRepository {

  override suspend fun getRoundTripQuote(
    mooveAmount: BigDecimal,
    tolerancePercentage: BigDecimal
  ): RecoveryDexQuote {
    require(mooveAmount > BigDecimal.ZERO) { "MOOVE amount must be positive" }
    require(tolerancePercentage >= BigDecimal.ZERO && tolerancePercentage <= BigDecimal("100")) {
      "Tolerance must be between 0 and 100 percent"
    }

    val mooveAtomic = toAtomic(mooveAmount)
    val tolerance = tolerancePercentage
      .divide(BigDecimal("100"), 8, RoundingMode.HALF_UP)
      .toDouble()

    val buy = fetchRoute(
      XExchangeSwapVariables(
        amountOut = mooveAtomic,
        tokenInID = EGLD_IDENTIFIER,
        tokenOutID = CowCowConfig.MOOVE_TOKEN_ID,
        tolerance = tolerance
      )
    )

    val sell = fetchRoute(
      XExchangeSwapVariables(
        amountIn = mooveAtomic,
        tokenInID = CowCowConfig.MOOVE_TOKEN_ID,
        tokenOutID = EGLD_IDENTIFIER,
        tolerance = tolerance
      )
    )

    val buyCostEgld = fromAtomic(buy.amountIn)
    val expectedSellAtomic = sell.smartSwap?.amountOut ?: sell.amountOut
    val expectedSellReturnEgld = fromAtomic(expectedSellAtomic)
    val minimumSellReturnEgld = expectedSellReturnEgld
      .multiply(BigDecimal.ONE.subtract(BigDecimal(tolerance.toString())))
      .max(BigDecimal.ZERO)

    return RecoveryDexQuote(
      mooveAmount = mooveAmount,
      buyCostEgld = buyCostEgld,
      expectedSellReturnEgld = expectedSellReturnEgld,
      minimumSellReturnEgld = minimumSellReturnEgld,
      tolerancePercentage = tolerancePercentage,
      buyMaxPriceDeviationPercent = buy.maxPriceDeviationPercent?.toPercentage(),
      sellMaxPriceDeviationPercent = sell.maxPriceDeviationPercent?.toPercentage()
    )
  }

  private suspend fun fetchRoute(variables: XExchangeSwapVariables): XExchangeSwapRoute {
    val response = api.getSwapQuote(
      XExchangeGraphQlRequest(variables = variables)
    )

    response.errors
      ?.firstOrNull()
      ?.let { error -> throw IllegalStateException(error.message) }

    return response.data?.swap
      ?: throw IllegalStateException("xExchange did not return a swap route")
  }

  private fun toAtomic(amount: BigDecimal): String = try {
    amount.movePointRight(DECIMALS).toBigIntegerExact().toString()
  } catch (error: ArithmeticException) {
    throw IllegalArgumentException("MOOVE amount supports at most 18 decimal places", error)
  }

  private fun fromAtomic(amount: String): BigDecimal =
    BigDecimal(amount).movePointLeft(DECIMALS)

  private fun Double.toPercentage(): BigDecimal =
    BigDecimal(toString()).multiply(BigDecimal("100"))

  private companion object {
    const val EGLD_IDENTIFIER = "EGLD"
    const val DECIMALS = 18
  }
}
