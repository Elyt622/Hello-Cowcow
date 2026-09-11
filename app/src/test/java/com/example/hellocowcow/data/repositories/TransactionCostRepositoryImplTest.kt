package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.data.network.api.MvxGatewayApi
import com.example.hellocowcow.data.network.api.NetworkConfig
import com.example.hellocowcow.data.network.api.NetworkConfigData
import com.example.hellocowcow.data.network.api.NetworkConfigResponse
import com.example.hellocowcow.data.network.api.TransactionCostData
import com.example.hellocowcow.data.network.api.TransactionCostRequest
import com.example.hellocowcow.data.network.api.TransactionCostResponse
import com.example.hellocowcow.domain.models.MvxTransaction
import java.math.BigDecimal
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionCostRepositoryImplTest {

  @Test
  fun `uses simulated gas and keeps signed gas limit as fee ceiling`() = runTest {
    val api = FakeGatewayApi(
      gasUnits = 300_000L,
      modifier = BigDecimal("0.01")
    )
    val repository = TransactionCostRepositoryImpl(api)
    val payload = "claimRewards"
    val transaction = MvxTransaction(
      nonce = 7,
      value = "0",
      receiver = "erd1contract",
      sender = "erd1sender",
      gasPrice = 1_000_000_000L,
      gasLimit = 30_000_000L,
      data = Base64.getEncoder().encodeToString(payload.toByteArray()),
      chainID = "1",
      version = 1
    )

    val estimate = repository.estimateFee(transaction)

    // movement gas = 50,000 + 12 * 1,500 = 68,000
    // simulated execution gas = 232,000; execution is charged at 1% modifier.
    assertEquals(0, estimate.feeEgld.compareTo(BigDecimal("0.00007032")))
    // If the signed 30M gas limit is fully charged, the same formula gives this ceiling.
    assertEquals(0, estimate.maxFeeEgld.compareTo(BigDecimal("0.00036732")))
    assertEquals(300_000L, estimate.gasUnits)
    assertEquals(30_000_000L, estimate.gasLimit)
    assertTrue(estimate.simulated)
  }

  @Test
  fun `rejects simulation above the gas limit that would actually be signed`() = runTest {
    val repository = TransactionCostRepositoryImpl(
      FakeGatewayApi(
        gasUnits = 30_000_001L,
        modifier = BigDecimal("0.01")
      )
    )
    val transaction = MvxTransaction(
      nonce = 7,
      value = "0",
      receiver = "erd1contract",
      sender = "erd1sender",
      gasPrice = 1_000_000_000L,
      gasLimit = 30_000_000L,
      data = Base64.getEncoder().encodeToString("claimRewards".toByteArray()),
      chainID = "1",
      version = 1
    )

    val error = runCatching {
      repository.estimateFee(transaction)
    }.exceptionOrNull()

    assertTrue(error is IllegalStateException)
    assertTrue(error?.message.orEmpty().contains("exceeds configured gas limit"))
  }

  @Test
  fun `rejects configured gas price below current network minimum`() = runTest {
    val repository = TransactionCostRepositoryImpl(
      FakeGatewayApi(
        gasUnits = 300_000L,
        modifier = BigDecimal("0.01"),
        minGasPrice = 1_000_000_001L
      )
    )
    val transaction = MvxTransaction(
      nonce = 7,
      value = "0",
      receiver = "erd1contract",
      sender = "erd1sender",
      gasPrice = 1_000_000_000L,
      gasLimit = 30_000_000L,
      data = Base64.getEncoder().encodeToString("claimRewards".toByteArray()),
      chainID = "1",
      version = 1
    )

    val error = runCatching {
      repository.estimateFee(transaction)
    }.exceptionOrNull()

    assertTrue(error is IllegalStateException)
    assertTrue(error?.message.orEmpty().contains("below current MultiversX minimum"))
  }

  @Test
  fun `falls back to configured gas limit when live estimate fails`() = runTest {
    val api = FakeGatewayApi(
      gasUnits = null,
      modifier = BigDecimal("0.01")
    )
    val repository = TransactionCostRepositoryImpl(api)
    val transaction = MvxTransaction(
      nonce = 7,
      value = "0",
      receiver = "erd1contract",
      sender = "erd1sender",
      gasPrice = 1_000_000_000L,
      gasLimit = 500_000L,
      data = Base64.getEncoder().encodeToString("ESDTTransfer@test@01".toByteArray()),
      chainID = "1",
      version = 1
    )

    val estimate = repository.estimateFee(transaction)

    assertEquals(500_000L, estimate.gasUnits)
    assertEquals(500_000L, estimate.gasLimit)
    assertEquals(0, estimate.feeEgld.compareTo(estimate.maxFeeEgld))
    assertFalse(estimate.simulated)
    assertTrue(estimate.feeEgld > BigDecimal.ZERO)
  }

  @Test
  fun `gas limit ceilings reproduce verified CowCow Explorer fees`() = runTest {
    val repository = TransactionCostRepositoryImpl(
      FakeGatewayApi(
        gasUnits = 1_000_000L,
        modifier = BigDecimal("0.01")
      )
    )

    val cases = listOf(
      Triple("claim@0788@0eb2@267e@0fde", 270_000_000L, BigDecimal("0.002786625")),
      Triple("unstake@0788@0eb2@267e@0fde", 315_000_000L, BigDecimal("0.003239595")),
      Triple("claimRewards", 600_000_000L, BigDecimal("0.00606732"))
    )

    cases.forEach { (payload, gasLimit, explorerFee) ->
      val transaction = MvxTransaction(
        nonce = 1,
        value = "0",
        receiver = "erd1contract",
        sender = "erd1sender",
        gasPrice = 1_000_000_000L,
        gasLimit = gasLimit,
        data = Base64.getEncoder().encodeToString(payload.toByteArray()),
        chainID = "1",
        version = 1
      )

      val estimate = repository.estimateFee(transaction)

      assertEquals(
        "Fee ceiling mismatch for $payload",
        0,
        estimate.maxFeeEgld.compareTo(explorerFee)
      )
    }
  }

  private class FakeGatewayApi(
    private val gasUnits: Long?,
    private val modifier: BigDecimal,
    private val minGasPrice: Long = 1_000_000_000L
  ) : MvxGatewayApi {

    override suspend fun getProcessStatus(txHash: String) =
      error("Not used in this test")

    override suspend fun estimateTransactionCost(
      request: TransactionCostRequest
    ): TransactionCostResponse = if (gasUnits != null) {
      TransactionCostResponse(
        data = TransactionCostData(txGasUnits = gasUnits.toString()),
        error = "",
        code = "successful"
      )
    } else {
      TransactionCostResponse(
        data = null,
        error = "simulation unavailable",
        code = "error"
      )
    }

    override suspend fun getNetworkConfig(): NetworkConfigResponse =
      NetworkConfigResponse(
        data = NetworkConfigData(
          config = NetworkConfig(
            minGasLimit = 50_000L,
            minGasPrice = minGasPrice,
            gasPerDataByte = 1_500L,
            gasPriceModifier = modifier,
            denomination = 18
          )
        ),
        error = "",
        code = "successful"
      )
  }
}
