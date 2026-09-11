package com.example.hellocowcow.data.retrofit.xoxnoApi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionStatsResponseTest {

  @Test
  fun `maps XOXNO collection stats to domain model`() {
    val response = CollectionStatsResponse(
      collection = "COW-cd463d",
      floorPrice = 2.4,
      listedCount = 42,
      tradingStats = TradingStatisticsResponse(
        totalVolume = 1234.5,
        totalTrades = 321,
        averagePrice = 3.8,
        allTimeHigh = AllTimeHighResponse(price = 17.5),
        day = TradingDataSummaryResponse(volume = 25.0),
        week = TradingDataSummaryResponse(volume = 140.0)
      ),
      collectionInfo = CollectionInfoResponse(
        holdersCount = 812,
        followCount = 430
      )
    )

    val domain = response.toDomain()

    assertEquals("COW-cd463d", domain.collection)
    assertEquals(2.4, domain.floorPrice!!, 0.0001)
    assertEquals(42, domain.listedCount)
    assertEquals(812, domain.holdersCount)
    assertEquals(430, domain.followCount)
    assertEquals(1234.5, domain.totalVolume, 0.0001)
    assertEquals(321, domain.totalTrades)
    assertEquals(3.8, domain.averagePrice!!, 0.0001)
    assertEquals(17.5, domain.allTimeHighPrice, 0.0001)
    assertEquals(25.0, domain.dayVolume, 0.0001)
    assertEquals(140.0, domain.weekVolume, 0.0001)
  }

  @Test
  fun `keeps optional collection metadata nullable`() {
    val domain = CollectionStatsResponse(
      collection = "COW-cd463d"
    ).toDomain()

    assertNull(domain.holdersCount)
    assertNull(domain.followCount)
  }
}
