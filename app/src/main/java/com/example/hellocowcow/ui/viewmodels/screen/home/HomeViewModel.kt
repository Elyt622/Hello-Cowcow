package com.example.hellocowcow.ui.viewmodels.screen.home

import com.example.hellocowcow.app.module.BaseViewModel
import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Resources
import com.example.hellocowcow.domain.repositories.NftRepository
import com.example.hellocowcow.domain.repositories.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
  private val tokenRepository: TokenRepository,
  val nftRepository: NftRepository
) : BaseViewModel() {

  private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
  val uiState: StateFlow<UiState> = _uiState

  private val _uiStateSold: MutableStateFlow<UiStateSold> = MutableStateFlow(UiStateSold.Loading)
  val uiStateSold: StateFlow<UiStateSold> = _uiStateSold

  data class SomeStats(
    var stakedCount: String,
    var listedCount: String,
    var floorPrice: String,
    var totalEgldVolume: Double?,
    var moovePrice: Double?,
    var mooveMC: Double?
  )

  sealed class UiState {
    data object Loading : UiState()
    data class Success(val data: SomeStats) : UiState()
    data class Error(val error: String) : UiState()
  }

  sealed class UiStateSold {
    data object Loading : UiStateSold()
    data class Success(val data: ArrayList<Resources>) : UiStateSold()
    data class Error(val error: String) : UiStateSold()
  }

  init {
    getSomeStats()
    getLastCowsSold()
  }

  private fun getSomeStats() =
    Observable.zip(
      tokenRepository.getToken(CowCowConfig.MOOVE_TOKEN_ID).toObservable(),
      nftRepository.getStakingCowsCount().toObservable(),
      nftRepository.getStatsCollection(CowCowConfig.COLLECTION_ID)
    ) { token, stakingCount, statsCowCollection ->
      SomeStats(
        moovePrice = token.price,
        mooveMC = token.marketCap,
        stakedCount = stakingCount.toString(),
        listedCount = statsCowCollection.listedCount.toString(),
        floorPrice = statsCowCollection.floorPrice.toString(),
        totalEgldVolume = statsCowCollection.totalVolume
      )
    }.subscribeBy(
      onNext = {
        _uiState.value = UiState.Success(it)
      },
      onError = {
        _uiState.value = UiState.Error(it.message.toString())
      }
    ).addTo(disposable)

  private fun getLastCowsSold() =
    nftRepository.getLastTenSold()
      .subscribeBy(
        onNext = {
          _uiStateSold.value = UiStateSold.Success(it)
        },
        onError = {
          _uiStateSold.value = UiStateSold.Error(it.message.toString())
        }
      ).addTo(disposable)

}
