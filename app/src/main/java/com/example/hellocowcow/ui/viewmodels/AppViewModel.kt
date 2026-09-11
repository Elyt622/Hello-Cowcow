package com.example.hellocowcow.ui.viewmodels

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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
  private val walletClient: WalletClient,
  private val accountRepository: AccountRepository
) : ViewModel() {

  sealed interface UiState {
    data object Guest : UiState
    data object Connecting : UiState
    data class Connected(
      val account: DomainAccount,
      val topic: String
    ) : UiState
    data class Error(val message: String) : UiState
  }

  sealed interface Effect {
    data class OpenXPortal(val pairingUri: String) : Effect
  }

  private val _uiState = MutableStateFlow<UiState>(UiState.Guest)
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
  val effects: SharedFlow<Effect> = _effects.asSharedFlow()

  init {
    observeWalletEvents()
  }

  fun refreshSession() {
    viewModelScope.launch {
      runCatching {
        walletClient.getActiveSession()
      }.onSuccess { session ->
        if (session == null) {
          _uiState.value = UiState.Guest
        } else {
          val current = _uiState.value
          if (current is UiState.Connected &&
            current.topic == session.topic &&
            current.account.address == session.address
          ) {
            return@onSuccess
          }
          loadSession(session)
        }
      }.onFailure { error ->
        if (_uiState.value !is UiState.Connected) {
          _uiState.value = UiState.Error(
            error.message ?: "Unable to restore xPortal session"
          )
        }
      }
    }
  }

  fun connectWallet() {
    if (_uiState.value is UiState.Connecting) return

    _uiState.value = UiState.Connecting

    walletClient.connect(
      sessionExpiryEpochSeconds = getSessionExpiryEpochSeconds(),
      onSuccess = { pairingUri ->
        _effects.tryEmit(Effect.OpenXPortal(pairingUri))
      },
      onError = { message ->
        _uiState.value = UiState.Error(message)
      }
    )
  }

  fun continueAsGuest() {
    _uiState.value = UiState.Guest
  }

  private fun observeWalletEvents() {
    viewModelScope.launch {
      walletClient.events.collect { event ->
        when (event) {
          is WalletEvent.SessionApproved -> loadSession(event.session)
          WalletEvent.SessionDisconnected -> _uiState.value = UiState.Guest
          is WalletEvent.ConnectionError -> {
            if (_uiState.value !is UiState.Connected) {
              _uiState.value = UiState.Error(event.message)
            }
          }
          is WalletEvent.TransactionSignatureResult,
          is WalletEvent.TransactionSignatureError,
          is WalletEvent.RequestExpired -> Unit
        }
      }
    }
  }

  private suspend fun loadSession(session: WalletSession) {
    _uiState.value = UiState.Connecting

    runCatching {
      accountRepository.getAccount(session.address)
    }.onSuccess { account ->
      _uiState.value = UiState.Connected(
        account = account,
        topic = session.topic
      )
    }.onFailure { error ->
      _uiState.value = UiState.Error(
        error.message ?: "Unable to load connected MultiversX account"
      )
    }
  }

  private fun getSessionExpiryEpochSeconds(): Long =
    (System.currentTimeMillis() / 1000) +
        TimeUnit.SECONDS.convert(7, TimeUnit.DAYS)
}
