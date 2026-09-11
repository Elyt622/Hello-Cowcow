package com.example.hellocowcow.domain.repositories

interface RewardsRepository {
  suspend fun getUserData(address: String, forceRefresh: Boolean = false): String
}
