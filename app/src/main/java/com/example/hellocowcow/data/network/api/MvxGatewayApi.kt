package com.example.hellocowcow.data.network.api

import com.example.hellocowcow.data.retrofit.mvxGateway.response.ProcessStatusResponse
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MvxGatewayApi {

  @GET("transaction/{txHash}/process-status")
  suspend fun getProcessStatus(
    @Path("txHash") txHash: String
  ): ProcessStatusResponse

  @POST("transaction/cost")
  suspend fun estimateTransactionCost(
    @Body request: TransactionCostRequest
  ): TransactionCostResponse

  @GET("network/config")
  suspend fun getNetworkConfig(): NetworkConfigResponse
}

data class TransactionCostRequest(
  val nonce: Int,
  val value: String,
  val receiver: String,
  val sender: String,
  val chainID: String,
  val version: Int,
  val options: Int? = null,
  val guardian: String? = null,
  val data: String? = null
)

data class TransactionCostResponse(
  val data: TransactionCostData? = null,
  val error: String? = null,
  val code: String? = null
)

data class TransactionCostData(
  val txGasUnits: String? = null
)

data class NetworkConfigResponse(
  val data: NetworkConfigData? = null,
  val error: String? = null,
  val code: String? = null
)

data class NetworkConfigData(
  val config: NetworkConfig? = null
)

data class NetworkConfig(
  @SerializedName("erd_min_gas_limit")
  val minGasLimit: Long? = null,
  @SerializedName("erd_min_gas_price")
  val minGasPrice: Long? = null,
  @SerializedName("erd_gas_per_data_byte")
  val gasPerDataByte: Long? = null,
  @SerializedName("erd_gas_price_modifier")
  val gasPriceModifier: BigDecimal? = null,
  @SerializedName("erd_denomination")
  val denomination: Int? = null
)
