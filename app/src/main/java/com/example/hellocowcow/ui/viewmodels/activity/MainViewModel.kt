package com.example.hellocowcow.ui.viewmodels.activity

import androidx.lifecycle.viewModelScope
import com.example.hellocowcow.app.module.BaseViewModel
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.repositories.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  private val accountRepository: AccountRepository
) : BaseViewModel() {

  private val _currentAccount: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
  val currentAccount: StateFlow<UiState> get() = _currentAccount

  sealed class UiState {
    data object Loading : UiState()
    data class Success(val data: DomainAccount) : UiState()
    data class Error(val error: String) : UiState()
  }

  fun getAccount(address: String) {
    viewModelScope.launch {
      _currentAccount.value = UiState.Loading

      runCatching {
        accountRepository.getAccount(address)
      }.onSuccess { account ->
        _currentAccount.value = UiState.Success(account)
      }.onFailure { error ->
        _currentAccount.value = UiState.Error(
          error.message ?: error.toString()
        )
      }
    }
  }

}
