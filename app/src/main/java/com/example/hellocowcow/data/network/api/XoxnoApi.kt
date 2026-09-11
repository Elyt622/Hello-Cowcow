package com.example.hellocowcow.data.network.api

import com.example.hellocowcow.data.retrofit.xoxnoApi.CollectionStatsResponse
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface XoxnoApi {

  @GET("/collection/{collection}/stats")
  fun getStatsCollection(
    @Path("collection") collection: String
  ): Observable<CollectionStatsResponse>

}
