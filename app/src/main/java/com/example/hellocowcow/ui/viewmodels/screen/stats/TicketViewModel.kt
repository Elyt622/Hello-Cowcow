package com.example.hellocowcow.ui.viewmodels.screen.stats

import androidx.lifecycle.ViewModel
import com.example.hellocowcow.domain.models.TicketCollection
import com.example.hellocowcow.domain.repositories.NftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TicketViewModel @Inject constructor(
    private val nftRepository: NftRepository
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val data: TicketCollection) : UiState()
        data class Error(val error: String) : UiState()
    }

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        getTicketCollectionStats()
    }

    private fun getTicketCollectionStats() {
        nftRepository.getStatsCollection(
            "TICKET-231cd2"
        ).map { it.pageProps!! }
            .map { stats ->
                TicketCollection(
                    holdersCount = stats.holdersCount,
                    listedCount = stats.listedNFTs,
                    floorPrice = stats.fallBackFloor,
                    athEgldPrice = stats.profileFallback?.statistics?.tradeData?.athEgldPrice,
                    totalTrades = stats.profileFallback?.statistics?.tradeData?.totalTrades,
                    followAccountsCount = stats.profileFallback?.statistics?.other?.followCount,
                    dayEgldVolume = stats.profileFallback?.statistics?.tradeData?.dayEgldVolume,
                    weekEgldVolume = stats.profileFallback?.statistics?.tradeData?.weekEgldVolume,
                    totalEgldVolume = stats.profileFallback?.statistics?.tradeData?.totalEgldVolume,
                    ticketsUsed = 0
                )
        }
            .subscribeBy(
                onNext = {
                    _uiState.value = UiState.Success(it)
                },
                onError = {
                    _uiState.value = UiState.Error(it.message.toString())
                }
        ).isDisposed
    }

}
