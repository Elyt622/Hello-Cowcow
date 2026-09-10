package com.example.hellocowcow.domain.models

data class DomainCollectionStats(
  val collection: String,
  val floorPrice: Double?,
  val listedCount: Int,
  val holdersCount: Int?,
  val followCount: Int?,
  val totalVolume: Double,
  val totalTrades: Int,
  val averagePrice: Double?,
  val allTimeHighPrice: Double,
  val dayVolume: Double,
  val weekVolume: Double
)
