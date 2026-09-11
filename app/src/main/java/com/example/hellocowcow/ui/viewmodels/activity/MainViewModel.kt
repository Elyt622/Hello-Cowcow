package com.example.hellocowcow.ui.viewmodels.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocowcow.core.wallet.WalletClient
import com.example.hellocowcow.core.wallet.WalletEvent
import com.example.hellocowcow.core.wallet.WalletSession
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.repositories.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  private val accountRepository: AccountRepository,
  private val walletClient: WalletClient
) : ViewModel() {

  sealed interface WalletUiState {
    data object CheckingSession : WalletUiState
    data object Disconnected : WalletUiState
    data object Connecting : WalletUiState
    data class LoadingAccount(val address: String) : WalletUiState
    data class Connected(
      val account: DomainAccount,
      val topic: String
    ) : WalletUiState
    data class Error(val message: String) : WalletUiState
  }

  sealed interface UiEffect {
    data class OpenXPortal(val pairingUri: String) : UiEffect
  }

  private val _walletState = MutableStateFlow<WalletUiState>(WalletUiState.CheckingSession)
  val walletState: StateFlow<WalletUiState> = _walletState.asStateFlow()

  private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 4)
  val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

  init {
    observeWalletEvents()
    refreshWalletSession()
  }

  fun refreshWalletSession() {
    viewModelScope.launch {
      val keepCurrentContent = _walletState.value is WalletUiState.Connected
      if (!keepCurrentContent) {
        _walletState.value = WalletUiState.CheckingSession
      }

      runCatching {
        walletClient.getActiveSession()
      }.onSuccess { session ->
        if (session == null) {
          _walletState.value = WalletUiState.Disconnected
        } else {
          loadAccount(
            session = session,
            showLoading = !keepCurrentContent
          )
        }
      }.onFailure {
        if (!keepCurrentContent) {
          _walletState.value = WalletUiState.Disconnected
        }
      }
    }
  }

  fun connectWallet() {
    if (_walletState.value is WalletUiState.Connecting) return

    _walletState.value = WalletUiState.Connecting
    walletClient.connect(
      sessionExpiryEpochSeconds = currentEpochSeconds() + WALLET_SESSION_SECONDS,
      onSuccess = { pairingUri ->
        if (!_effects.tryEmit(UiEffect.OpenXPortal(pairingUri))) {
          _walletState.value = WalletUiState.Error("Unable to open xPortal")
        }
      },
      onError = { message ->
        _walletState.value = WalletUiState.Error(message)
      }
    )
  }

  private fun observeWalletEvents() {
    viewModelScope.launch {
      walletClient.events.collect { event ->
        when (event) {
          WalletEvent.Ready -> refreshWalletSession()

          is WalletEvent.SessionApproved -> loadAccount(event.session)

          is WalletEvent.SessionDisconnected -> {
            _walletState.value = WalletUiState.Disconnected
          }

          is WalletEvent.ConnectionError -> {
            if (
              _walletState.value is WalletUiState.Connecting ||
              _walletState.value is WalletUiState.CheckingSession
            ) {
              _walletState.value = WalletUiState.Error(event.message)
            }
          }

          is WalletEvent.TransactionSignatureResult,
          is WalletEvent.TransactionSignatureError,
          is WalletEvent.RequestExpired -> Unit
        }
      }
    }
  }

  private suspend fun loadAccount(
    session: WalletSession,
    showLoading: Boolean = true
  ) {
    if (showLoading) {
      _walletState.value = WalletUiState.LoadingAccount(session.address)
    }

    runCatching {
      accountRepository.getAccount(session.address)
    }.onSuccess { account ->
      _walletState.value = WalletUiState.Connected(
        account = account,
        topic = session.topic
      )
    }.onFailure { error ->
      _walletState.value = WalletUiState.Error(
        error.message ?: "Unable to load your MultiversX account"
      )
    }
  }

  private fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000L

  private companion object {
    const val WALLET_SESSION_SECONDS = 7L * 24L * 60L * 60L
  }
}
