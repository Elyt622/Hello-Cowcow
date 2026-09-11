package com.example.hellocowcow.domain.repositories

interface RewardsRepository {
  suspend fun getUserData(address: String): String
}
