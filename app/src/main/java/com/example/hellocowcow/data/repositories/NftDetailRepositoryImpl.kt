package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.data.network.api.MvxApi
import com.example.hellocowcow.domain.models.DomainNft
import com.example.hellocowcow.domain.repositories.NftDetailRepository
import javax.inject.Inject

class NftDetailRepositoryImpl @Inject constructor(
  private val mvxApi: MvxApi
) : NftDetailRepository {

  override suspend fun getNft(identifier: String): DomainNft =
    mvxApi.getNft(identifier).blockingGet().toDomain()
}
