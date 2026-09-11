package com.example.hellocowcow.data.network.api

import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Activity
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Collection
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Nft
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Upgraded
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface ProxyXoxnoApi {

  @GET("/nft/{identifier}")
  fun getNft(
    @Path("identifier") identifier: String
  ): Single<Nft>

  @GET("/nft/{identifier}")
  suspend fun getNftDetail(
    @Path("identifier") identifier: String
  ): Nft

  // Legacy endpoints kept only for compatibility while their DTOs are migrated.
  // Do not call these from 2026 startup paths; current XOXNO uses /user, /nft/query
  // and /activity/query instead of the old /accounts and encoded proxy routes.
  @GET("/accounts/{address}/listings")
  fun getCowsListing(
    @Path("address") address: String
  ): Observable<Collection>

  @GET("/accounts/{address}/inventory")
  fun getCowsInWallet(
    @Path("address") address: String
  ): Observable<Collection>

  @GET("/searchNFTs/eyJmaWx0ZXJzIjp7Imhhc1NlY29uZE5GVCI6dHJ1ZX0sImNvbGxlY3Rpb24iOiJDT1ctY2Q0NjNkIiwidG9wIjoxfQ==")
  fun getUpgradedCowsCount(): Observable<Upgraded>

  @GET("/getTradingActivity/eyJmaWx0ZXJzIjp7ImNvbGxlY3Rpb24iOlsiQ09XLWNkNDYzZCJdLCJ0b2tlbnMiOltdLCJhY3Rpb24iOltdLCJyYW5nZSI6eyJtaW4iOjAsIm1heCI6OTAwNzE5OTI1NDc0MDk5MSwidHlwZSI6InByaWNlIn0sImF0dHJpYnV0ZXMiOltdLCJtYXJrZXRwbGFjZSI6W119LCJ0b3AiOjEwLCJza2lwIjowLCJzZWxlY3QiOltdLCJvcmRlckJ5IjpbInRpbWVzdGFtcCBkZXNjIl19")
  fun getLastTenSold(): Observable<Activity>
}
