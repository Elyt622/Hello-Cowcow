package com.example.hellocowcow.ui.viewmodels.screen.stats

import com.example.hellocowcow.app.module.BaseViewModel
import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.domain.models.TicketCollection
import com.example.hellocowcow.domain.repositories.NftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TicketViewModel @Inject constructor(
  private val nftRepository: NftRepository
) : BaseViewModel() {

  sealed class UiState {
    data object Loading : UiState()
    data class Success(val data: TicketCollection) : UiState()
    data class Error(val error: String) : UiState()
  }

  private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
  val uiState: StateFlow<UiState> = _uiState

  init {
    getTicketCollectionStats()
  }

  private fun getTicketCollectionStats() =
    Observable.zip(
      nftRepository.getStatsCollection(CowCowConfig.TICKET_COLLECTION_ID),
      nftRepository.getTicketsUsedCount().toObservable()
    ) { stats, ticketUsed ->
      TicketCollection(
        holdersCount = stats.holdersCount,
        listedCount = stats.listedCount,
        floorPrice = stats.floorPrice,
        athEgldPrice = stats.allTimeHighPrice,
        totalTrades = stats.totalTrades,
        followAccountsCount = stats.followCount,
        dayEgldVolume = stats.dayVolume,
        weekEgldVolume = stats.weekVolume,
        totalEgldVolume = stats.totalVolume,
        ticketsUsed = ticketUsed
      )
    }.subscribeBy(
      onNext = {
        _uiState.value = UiState.Success(it)
      },
      onError = {
        _uiState.value = UiState.Error(it.message.toString())
      }
    ).addTo(disposable)

}
