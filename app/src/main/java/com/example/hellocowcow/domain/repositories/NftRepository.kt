package com.example.hellocowcow.domain.repositories

import com.example.hellocowcow.data.retrofit.mvxApi.request.Reward
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Collection
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Resources
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Upgraded
import com.example.hellocowcow.domain.models.DomainCollectionStats
import com.example.hellocowcow.domain.models.DomainNft
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface NftRepository {

  fun getAllCowsInWallet(
    address: String
  ): Single<List<DomainNft>>

  fun getNftMvx(
    identifier: String
  ): Single<DomainNft>

  fun getAllDataUsers(
    request: Reward
  ): Observable<com.example.hellocowcow.data.retrofit.mvxApi.response.Reward>

  fun getCowsWithCollection(
    identifiers: String,
    size: Int,
    from: Int
  ): Single<List<DomainNft>>

  fun getNftXoxno(
    identifier: String
  ): Single<DomainNft>

  fun getCowsListing(
    address: String
  ): Observable<Collection>

  fun getCowsInWallet(
    address: String
  ): Observable<Collection>

  fun getUpgradedCowsCount(): Observable<Upgraded>

  fun getStakingCowsCount(): Single<Int>

  fun getStatsCollection(
    collection: String
  ): Observable<DomainCollectionStats>

  fun getTicketsUsedCount(): Single<Int>

  fun getLastTenSold(): Observable<ArrayList<Resources>>

}
