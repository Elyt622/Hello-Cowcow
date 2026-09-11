package com.example.hellocowcow.data.network.api

import com.example.hellocowcow.data.retrofit.mvxApi.request.Reward as RewardRequest
import com.example.hellocowcow.data.retrofit.mvxApi.request.Transaction
import com.example.hellocowcow.data.retrofit.mvxApi.response.Account
import com.example.hellocowcow.data.retrofit.mvxApi.response.Nft
import com.example.hellocowcow.data.retrofit.mvxApi.response.Reward as RewardResponse
import com.example.hellocowcow.data.retrofit.mvxApi.response.Token
import com.example.hellocowcow.data.retrofit.mvxApi.response.Transactions
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MvxApi {

  @GET("/tokens/{identifier}")
  fun getToken(
    @Path("identifier") identifier: String
  ): Single<Token>

  @GET("/nfts/{identifier}")
  fun getNft(
    @Path("identifier") identifier: String
  ): Single<Nft>

  @GET("/accounts/{address}/nfts?collections=COW-cd463d")
  fun getAllCowsInWallet(
    @Path("address") address: String
  ): Single<List<Nft>>

  @GET("/collections/COW-cd463d/nfts")
  fun getCowsWithCollection(
    @Query("identifiers") identifiers: String,
    @Query("size") size: Int,
    @Query("from") from: Int
  ): Single<List<Nft>>

  @GET("/accounts/{address}?withGuardianInfo=true")
  suspend fun getAccount(
    @Path("address") address: String
  ): Account

  @GET("/accounts/erd1qqqqqqqqqqqqqpgqqgzzsl0re9e3u0t3mhv3jwg6zu63zssd7yqs3uu9jk/nfts/count?collection=COW-cd463d")
  fun getStakingCowsCount(): Single<Int>

  @GET("/collections/TICKET-231cd2/nfts/count?traits=Chapter%204:Used")
  fun getTicketsUsedCount(): Single<Int>

  @POST("/query")
  fun getTotalRewardsToCollect(
    @Body reward: RewardRequest
  ): Single<RewardResponse>

  @POST("/query")
  fun getAllDataUsers(
    @Body request: RewardRequest
  ): Observable<RewardResponse>

  @POST("/query")
  suspend fun queryContract(
    @Body request: RewardRequest
  ): RewardResponse

  @POST("/transactions")
  suspend fun sendTransaction(
    @Body tx: Transaction
  ): Transactions
}
