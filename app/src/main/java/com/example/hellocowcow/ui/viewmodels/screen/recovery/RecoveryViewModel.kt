package com.example.hellocowcow.ui.viewmodels.screen.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocowcow.domain.models.RecoverySnapshot
import com.example.hellocowcow.domain.repositories.RecoveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryViewModel @Inject constructor(
  private val recoveryRepository: RecoveryRepository
) : ViewModel() {

  sealed interface UiState {
    data object Loading : UiState
    data class Success(val snapshot: RecoverySnapshot) : UiState
    data class Error(val message: String) : UiState
  }

  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState: StateFlow<UiState> = _uiState

  fun load(address: String) {
    if (address.isBlank()) {
      _uiState.value = UiState.Error("Connect xPortal to run the recovery diagnostic")
      return
    }

    _uiState.value = UiState.Loading
    viewModelScope.launch {
      runCatching { recoveryRepository.getSnapshot(address) }
        .onSuccess { snapshot ->
          _uiState.value = UiState.Success(snapshot)
        }
        .onFailure { error ->
          _uiState.value = UiState.Error(
            error.message ?: "Unable to load the CowCow recovery diagnostic"
          )
        }
    }
  }
}
