package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.network.api.XExchangeGraphQlData
import com.example.hellocowcow.data.network.api.XExchangeGraphQlRequest
import com.example.hellocowcow.data.network.api.XExchangeGraphQlResponse
import com.example.hellocowcow.data.network.api.XExchangeQuoteApi
import com.example.hellocowcow.data.network.api.XExchangeSwapRoute
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecoveryDexQuoteRepositoryImplTest {

  @Test
  fun `builds fixed output buy and fixed input sell quotes`() = runTest {
    val requests = mutableListOf<XExchangeGraphQlRequest>()
    val api = object : XExchangeQuoteApi {
      override suspend fun getSwapQuote(request: XExchangeGraphQlRequest): XExchangeGraphQlResponse {
        requests += request
        val route = if (request.variables.tokenInID == "EGLD") {
          XExchangeSwapRoute(
            amountIn = "100000000000000000",
            amountOut = "10000000000000000000",
            tokenInID = "EGLD",
            tokenOutID = CowCowConfig.MOOVE_TOKEN_ID,
            maxPriceDeviationPercent = 0.002
          )
        } else {
          XExchangeSwapRoute(
            amountIn = "10000000000000000000",
            amountOut = "98000000000000000",
            tokenInID = CowCowConfig.MOOVE_TOKEN_ID,
            tokenOutID = "EGLD",
            maxPriceDeviationPercent = 0.003
          )
        }
        return XExchangeGraphQlResponse(data = XExchangeGraphQlData(swap = route))
      }
    }

    val repository = RecoveryDexQuoteRepositoryImpl(api)
    val quote = repository.getRoundTripQuote(
      mooveAmount = BigDecimal("10"),
      tolerancePercentage = BigDecimal.ONE
    )

    assertEquals(2, requests.size)

    val buyVariables = requests[0].variables
    assertEquals(null, buyVariables.amountIn)
    assertEquals("10000000000000000000", buyVariables.amountOut)
    assertEquals("EGLD", buyVariables.tokenInID)
    assertEquals(CowCowConfig.MOOVE_TOKEN_ID, buyVariables.tokenOutID)
    assertEquals(0.01, buyVariables.tolerance, 0.0)

    val sellVariables = requests[1].variables
    assertEquals("10000000000000000000", sellVariables.amountIn)
    assertEquals(null, sellVariables.amountOut)
    assertEquals(CowCowConfig.MOOVE_TOKEN_ID, sellVariables.tokenInID)
    assertEquals("EGLD", sellVariables.tokenOutID)

    assertEquals(BigDecimal("0.100000000000000000"), quote.buyCostEgld)
    assertEquals(BigDecimal("0.098000000000000000"), quote.expectedSellReturnEgld)
    assertEquals(BigDecimal("0.097029702970297030"), quote.minimumSellReturnEgld)
    assertEquals(BigDecimal("0.200"), quote.buyMaxPriceDeviationPercent)
    assertEquals(BigDecimal("0.300"), quote.sellMaxPriceDeviationPercent)
  }

  @Test
  fun `uses smart swap output when present`() = runTest {
    val api = object : XExchangeQuoteApi {
      var call = 0
      override suspend fun getSwapQuote(request: XExchangeGraphQlRequest): XExchangeGraphQlResponse {
        call += 1
        return if (call == 1) {
          XExchangeGraphQlResponse(
            data = XExchangeGraphQlData(
              swap = XExchangeSwapRoute(
                amountIn = "100000000000000000",
                amountOut = "10000000000000000000",
                tokenInID = "EGLD",
                tokenOutID = CowCowConfig.MOOVE_TOKEN_ID
              )
            )
          )
        } else {
          XExchangeGraphQlResponse(
            data = XExchangeGraphQlData(
              swap = XExchangeSwapRoute(
                amountIn = "10000000000000000000",
                amountOut = "97000000000000000",
                tokenInID = CowCowConfig.MOOVE_TOKEN_ID,
                tokenOutID = "EGLD",
                smartSwap = com.example.hellocowcow.data.network.api.XExchangeSmartSwap(
                  amountOut = "98000000000000000"
                )
              )
            )
          )
        }
      }
    }

    val quote = RecoveryDexQuoteRepositoryImpl(api).getRoundTripQuote(BigDecimal("10"))

    assertEquals(BigDecimal("0.098000000000000000"), quote.expectedSellReturnEgld)
  }
}
