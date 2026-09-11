package com.example.hellocowcow.data.network.api

import com.example.hellocowcow.data.retrofit.mvxGateway.response.ProcessStatusResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface MvxGatewayApi {

  @GET("transaction/{txHash}/process-status")
  suspend fun getProcessStatus(
    @Path("txHash") txHash: String
  ): ProcessStatusResponse
}
