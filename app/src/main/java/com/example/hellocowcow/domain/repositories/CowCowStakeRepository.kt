package com.example.hellocowcow.domain.repositories

interface CowCowStakeRepository {
  suspend fun getStakedCowNonces(address: String): List<String>
}
