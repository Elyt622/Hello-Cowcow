package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.data.network.api.MvxGatewayApi
import com.example.hellocowcow.data.network.api.NetworkConfig
import com.example.hellocowcow.data.network.api.TransactionCostRequest
import com.example.hellocowcow.domain.models.MvxTransaction
import com.example.hellocowcow.domain.recovery.TransactionFeeEstimate
import com.example.hellocowcow.domain.repositories.TransactionCostRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Base64
import javax.inject.Inject

class TransactionCostRepositoryImpl @Inject constructor(
  private val api: MvxGatewayApi
) : TransactionCostRepository {

  override suspend fun estimateFee(transaction: MvxTransaction): TransactionFeeEstimate {
    val config = getNetworkConfig()
    val minGasPrice = requireNotNull(config.minGasPrice) { "Network min gas price is missing" }
    check(transaction.gasPrice >= minGasPrice) {
      "Configured gas price ${transaction.gasPrice} is below current MultiversX minimum $minGasPrice"
    }
    require(transaction.gasLimit > 0) { "Transaction gas limit must be positive" }

    var simulationError: String? = null
    val simulatedGas = runCatching {
      val response = api.estimateTransactionCost(
        TransactionCostRequest(
          nonce = transaction.nonce,
          value = transaction.value,
          receiver = transaction.receiver,
          sender = transaction.sender,
          gasPrice = transaction.gasPrice,
          gasLimit = transaction.gasLimit,
          chainID = transaction.chainID,
          version = transaction.version,
          options = transaction.options,
          guardian = transaction.guardian,
          data = transaction.data
        )
      )

      check(response.error.isNullOrBlank()) {
        response.error ?: "MultiversX transaction cost estimation failed"
      }

      val gasUnits = response.data?.txGasUnits?.toLongOrNull()
        ?: error("MultiversX did not return transaction gas units")

      check(gasUnits > 0L) {
        response.data?.returnMessage
          ?.takeIf { it.isNotBlank() }
          ?: "MultiversX returned 0 gas units for this transaction"
      }

      gasUnits
    }.onFailure { error ->
      simulationError = error.message ?: error::class.java.simpleName
    }.getOrNull()

    val expectedGasUnits = simulatedGas ?: transaction.gasLimit
    check(simulatedGas == null || simulatedGas <= transaction.gasLimit) {
      "Live MultiversX gas estimate $simulatedGas exceeds configured gas limit ${transaction.gasLimit}"
    }

    return TransactionFeeEstimate(
      feeEgld = calculateFeeEgld(
        transaction = transaction,
        gasUnits = expectedGasUnits,
        config = config
      ),
      maxFeeEgld = calculateFeeEgld(
        transaction = transaction,
        gasUnits = transaction.gasLimit,
        config = config
      ),
      gasUnits = expectedGasUnits,
      gasLimit = transaction.gasLimit,
      simulated = simulatedGas != null,
      simulationError = simulationError
    )
  }

  private suspend fun getNetworkConfig(): NetworkConfig {
    val response = api.getNetworkConfig()
    check(response.error.isNullOrBlank()) {
      response.error ?: "Unable to load MultiversX network configuration"
    }

    return requireNotNull(response.data?.config) {
      "MultiversX network configuration is missing"
    }
  }

  private fun calculateFeeEgld(
    transaction: MvxTransaction,
    gasUnits: Long,
    config: NetworkConfig
  ): BigDecimal {
    val minGasLimit = requireNotNull(config.minGasLimit) { "Network min gas limit is missing" }
    val gasPerDataByte = requireNotNull(config.gasPerDataByte) {
      "Network gas-per-data-byte is missing"
    }
    val gasPriceModifier = requireNotNull(config.gasPriceModifier) {
      "Network gas price modifier is missing"
    }
    val denomination = config.denomination ?: DEFAULT_DENOMINATION

    val dataBytes = transaction.data
      ?.takeIf { it.isNotBlank() }
      ?.let { Base64.getDecoder().decode(it).size.toLong() }
      ?: 0L

    val movementGas = minGasLimit + gasPerDataByte * dataBytes
    val totalGas = maxOf(gasUnits, movementGas)
    val executionGas = (totalGas - movementGas).coerceAtLeast(0L)
    val gasPrice = BigDecimal.valueOf(transaction.gasPrice)

    val movementFeeAtoms = BigDecimal.valueOf(movementGas).multiply(gasPrice)
    val executionFeeAtoms = BigDecimal.valueOf(executionGas)
      .multiply(gasPrice)
      .multiply(gasPriceModifier)

    return movementFeeAtoms
      .add(executionFeeAtoms)
      .movePointLeft(denomination)
      .setScale(FEE_SCALE, RoundingMode.HALF_UP)
      .stripTrailingZeros()
  }

  private companion object {
    const val DEFAULT_DENOMINATION = 18
    const val FEE_SCALE = 18
  }
}
