package com.example.hellocowcow.data.network.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

interface XExchangeQuoteApi {

  @POST("graphql")
  suspend fun getSwapQuote(
    @Body request: XExchangeGraphQlRequest
  ): XExchangeGraphQlResponse
}

data class XExchangeGraphQlRequest(
  val operationName: String = "swapPackageSwapRoute",
  val variables: XExchangeSwapVariables,
  val query: String = PUBLIC_SWAP_QUERY
)

data class XExchangeSwapVariables(
  val amountIn: String? = null,
  val amountOut: String? = null,
  val tokenInID: String,
  val tokenOutID: String,
  val tolerance: Double
)

data class XExchangeGraphQlResponse(
  val data: XExchangeGraphQlData? = null,
  val errors: List<XExchangeGraphQlError>? = null
)

data class XExchangeGraphQlData(
  val swap: XExchangeSwapRoute? = null
)

data class XExchangeSwapRoute(
  val amountIn: String,
  val amountOut: String,
  val tokenInID: String,
  val tokenOutID: String,
  val pricesImpact: List<Double> = emptyList(),
  val maxPriceDeviationPercent: Double? = null,
  val smartSwap: XExchangeSmartSwap? = null
)

data class XExchangeSmartSwap(
  val feeAmount: String? = null,
  val feePercentage: Double? = null,
  val amountOut: String? = null
)

data class XExchangeGraphQlError(
  val message: String
)

private const val PUBLIC_SWAP_QUERY = """
query swapPackageSwapRoute(
  ${'$'}amountIn: String
  ${'$'}amountOut: String
  ${'$'}tokenInID: String!
  ${'$'}tokenOutID: String!
  ${'$'}tolerance: Float!
) {
  swap(
    amountIn: ${'$'}amountIn
    amountOut: ${'$'}amountOut
    tokenInID: ${'$'}tokenInID
    tokenOutID: ${'$'}tokenOutID
    tolerance: ${'$'}tolerance
  ) {
    amountIn
    amountOut
    tokenInID
    tokenOutID
    pricesImpact
    maxPriceDeviationPercent
    smartSwap {
      feeAmount
      feePercentage
      amountOut
    }
  }
}
"""
