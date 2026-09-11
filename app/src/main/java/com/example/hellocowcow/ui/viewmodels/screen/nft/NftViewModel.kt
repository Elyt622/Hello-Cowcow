package com.example.hellocowcow.ui.viewmodels.screen.nft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocowcow.domain.models.DomainNft
import com.example.hellocowcow.domain.repositories.NftDetailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NftViewModel @Inject constructor(
  private val repository: NftDetailRepository
) : ViewModel() {

  sealed interface UiState {
    data object Loading : UiState
    data class Success(val nft: DomainNft) : UiState
    data class Error(val message: String) : UiState
  }

  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  fun load(identifier: String) {
    if (identifier.isBlank()) {
      _uiState.value = UiState.Error("NFT identifier is missing")
      return
    }

    _uiState.value = UiState.Loading
    viewModelScope.launch {
      runCatching {
        repository.getNft(identifier)
      }.onSuccess { nft ->
        _uiState.value = UiState.Success(nft)
      }.onFailure { error ->
        _uiState.value = UiState.Error(
          error.message ?: "Unable to load this CowCow"
        )
      }
    }
  }
}
