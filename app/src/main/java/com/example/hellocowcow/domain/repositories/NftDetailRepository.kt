package com.example.hellocowcow.domain.repositories

import com.example.hellocowcow.domain.models.DomainNft

interface NftDetailRepository {
  suspend fun getNft(identifier: String): DomainNft
}
