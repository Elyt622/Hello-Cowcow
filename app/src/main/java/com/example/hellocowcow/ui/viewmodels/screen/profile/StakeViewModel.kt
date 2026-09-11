package com.example.hellocowcow.ui.viewmodels.screen.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.hellocowcow.app.module.BaseViewModel
import com.example.hellocowcow.data.recovery.CowCowUserDataDecoder
import com.example.hellocowcow.data.retrofit.mvxApi.request.Reward
import com.example.hellocowcow.domain.models.DomainNft
import com.example.hellocowcow.domain.repositories.NftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class StakeViewModel @Inject constructor(
  private val nftRepository: NftRepository
) : BaseViewModel() {

  private val _address = mutableStateOf("")
  val address: State<String> get() = _address

  fun setAddress(value: String) {
    _address.value = value
  }

  sealed class UiState {
    data object NoData : UiState()
    data object Loading : UiState()
    data class Success(val data: List<DomainNft>) : UiState()
    data class Error(val error: String) : UiState()
  }

  private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
  val uiState: StateFlow<UiState> = _uiState

  fun getAllStakingCow() =
    getAllDataForUser()
      .map(CowCowUserDataDecoder::decodeStakedCowNonces)
      .map { nonces -> nonces.map { nonce -> "COW-cd463d-$nonce" } }
      .flatMapIterable { it }
      .flatMap { nft ->
        nftRepository.getNftXoxno(nft).toObservable()
      }.toList()
      .subscribeOn(Schedulers.io())
      .subscribeBy(
        onSuccess = { nfts ->
          if (nfts.isEmpty())
            _uiState.value = UiState.NoData
          else
            _uiState.value = UiState.Success(nfts)
        },
        onError = { error ->
          _uiState.value = UiState.Error(error.message.toString())
        }
      ).addTo(disposable)

  private fun getAllDataForUser(): Observable<String> =
    nftRepository.getAllDataUsers(
      Reward(
        "erd1qqqqqqqqqqqqqpgqqgzzsl0re9e3u0t3mhv3jwg6zu63zssd7yqs3uu9jk",
        "getAllDataForUser",
        "0",
        arrayListOf(),
        address.value
      )
    ).map { it.returnData[0] }
}
