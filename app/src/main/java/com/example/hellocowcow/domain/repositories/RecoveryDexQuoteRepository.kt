package com.example.hellocowcow.domain.repositories

import com.example.hellocowcow.domain.recovery.RecoveryDexQuote
import java.math.BigDecimal

interface RecoveryDexQuoteRepository {
  suspend fun getRoundTripQuote(
    mooveAmount: BigDecimal,
    tolerancePercentage: BigDecimal = BigDecimal.ONE
  ): RecoveryDexQuote
}
