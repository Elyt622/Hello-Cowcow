package com.example.hellocowcow.data.repositories

import com.example.hellocowcow.data.network.api.ProxyXoxnoApi
import com.example.hellocowcow.domain.models.DomainNft
import com.example.hellocowcow.domain.repositories.NftDetailRepository
import javax.inject.Inject

class NftDetailRepositoryImpl @Inject constructor(
  private val proxyXoxnoApi: ProxyXoxnoApi
) : NftDetailRepository {

  override suspend fun getNft(identifier: String): DomainNft =
    proxyXoxnoApi.getNftDetail(identifier).toDomain()
}
