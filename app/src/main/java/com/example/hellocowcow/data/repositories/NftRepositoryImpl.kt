package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.data.network.api.MvxApi
import com.example.hellocowcow.data.network.api.XoxnoApi
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Collection
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Resources
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Upgraded
import com.example.hellocowcow.domain.models.DomainCollectionStats
import com.example.hellocowcow.domain.models.DomainNft
import com.example.hellocowcow.domain.repositories.NftRepository
import com.example.hellocowcow.ui.viewmodels.util.MySchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class NftRepositoryImpl @Inject constructor(
  val mySchedulers: MySchedulers,
  val mvxApi: MvxApi,
  val xoxnoApi: XoxnoApi
) : NftRepository {

  override fun getAllCowsInWallet(
    address: String
  ): Single<List<DomainNft>> =
    mvxApi.getAllCowsInWallet(address)
      .toObservable()
      .flatMapIterable { it }
      .map { it.toDomain() }
      .toList()
      .subscribeOn(mySchedulers.io)
      .observeOn(mySchedulers.main)

  override fun getNftMvx(
    identifier: String
  ): Single<DomainNft> =
    mvxApi.getNft(identifier)
      .map { it.toDomain() }
      .subscribeOn(mySchedulers.io)
      .observeOn(mySchedulers.main)

  override fun getAllDataUsers(
    request: com.example.hellocowcow.data.retrofit.mvxApi.request.Reward
  ): Observable<com.example.hellocowcow.data.retrofit.mvxApi.response.Reward> =
    mvxApi.getAllDataUsers(request)
      .subscribeOn(mySchedulers.io)
      .observeOn(mySchedulers.main)

  override fun getCowsWithCollection(
    identifiers: String,
    size: Int,
    from: Int
  ): Single<List<DomainNft>> =
    mvxApi.getCowsWithCollection(identifiers, size, from)
      .toObservable()
      .flatMapIterable { it }
      .map { it.toDomain() }
      .toList()
      .subscribeOn(mySchedulers.io)
      .observeOn(mySchedulers.main)

  override fun getNftXoxno(
    identifier: String
  ): Single<DomainNft> =
    // Legacy name kept for compatibility. Modern NFT reads are sourced from
    // the public MultiversX API so they do not depend on obsolete XOXNO routes.
    getNftMvx(identifier)

  override fun getCowsListing(
    address: String
  ): Observable<Collection> =
    // The legacy /accounts/{address}/listings XOXNO endpoint was removed.
    // Keep this compatibility surface neutral until it is migrated to the
    // current public /nft/query filter model.
    Observable.just(Collection())
      .observeOn(mySchedulers.main)

  override fun getCowsInWallet(
    address: String
  ): Observable<Collection> =
    // Owned CowCows are already read through MultiversX by getAllCowsInWallet.
    // This legacy collection-shaped API is retained only for old UI callers.
    Observable.just(Collection())
      .observeOn(mySchedulers.main)

  override fun getUpgradedCowsCount(): Observable<Upgraded> =
    // The legacy encoded XOXNO /searchNFTs route no longer exists. Keep the
    // collection screen usable until this metric is migrated to /nft/query.
    Observable.just(Upgraded(count = null))
      .observeOn(mySchedulers.main)

  override fun getStakingCowsCount(): Single<Int> =
    mvxApi.getStakingCowsCount()
      .subscribeOn(mySchedulers.io)
      .observeOn(mySchedulers.main)

  override fun getStatsCollection(
    collection: String
  ): Observable<DomainCollectionStats> =
    xoxnoApi.getStatsCollection(collection)
      .map { it.toDomain() }
      .subscribeOn(mySchedulers.io)
      .observeOn(mySchedulers.main)

  override fun getTicketsUsedCount(): Single<Int> =
    mvxApi.getTicketsUsedCount()
      .subscribeOn(mySchedulers.io)
      .observeOn(mySchedulers.main)

  override fun getLastTenSold(): Observable<ArrayList<Resources>> =
    // The old encoded /getTradingActivity proxy endpoint was removed by XOXNO.
    // Returning an empty public state avoids a startup 404 while the model is
    // migrated to the current /activity/query response shape.
    Observable.just<ArrayList<Resources>>(arrayListOf())
      .observeOn(mySchedulers.main)
}
