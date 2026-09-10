package com.example.hellocowcow.data.retrofit.xoxnoApi

import com.example.hellocowcow.domain.models.DomainCollectionStats

data class CollectionStatsResponse(
  val collection: String = "",
  val floorPrice: Double? = null,
  val listedCount: Int = 0,
  val tradingStats: TradingStatisticsResponse = TradingStatisticsResponse(),
  val collectionInfo: CollectionInfoResponse? = null
) {
  fun toDomain(): DomainCollectionStats = DomainCollectionStats(
    collection = collection,
    floorPrice = floorPrice,
    listedCount = listedCount,
    holdersCount = collectionInfo?.holdersCount,
    followCount = collectionInfo?.followCount,
    totalVolume = tradingStats.totalVolume,
    totalTrades = tradingStats.totalTrades,
    averagePrice = tradingStats.averagePrice,
    allTimeHighPrice = tradingStats.allTimeHigh.price,
    dayVolume = tradingStats.day.volume,
    weekVolume = tradingStats.week.volume
  )
}

data class TradingStatisticsResponse(
  val totalVolume: Double = 0.0,
  val totalTrades: Int = 0,
  val averagePrice: Double? = null,
  val allTimeHigh: AllTimeHighResponse = AllTimeHighResponse(),
  val day: TradingDataSummaryResponse = TradingDataSummaryResponse(),
  val week: TradingDataSummaryResponse = TradingDataSummaryResponse()
)

data class TradingDataSummaryResponse(
  val volume: Double = 0.0,
  val volumeMargin: Double = 0.0,
  val trades: Int = 0,
  val tradesMargin: Double = 0.0,
  val minPrice: Double? = null,
  val maxPrice: Double? = null,
  val averagePrice: Double? = null
)

data class AllTimeHighResponse(
  val price: Double = 0.0,
  val timestamp: Long = 0,
  val txHash: String = "",
  val identifier: String = ""
)

data class CollectionInfoResponse(
  val name: String? = null,
  val collectionSize: Int? = null,
  val followCount: Int? = null,
  val holdersCount: Int? = null,
  val volume: Double? = null
)
