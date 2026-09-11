package com.example.hellocowcow.ui.viewmodels.activity

import androidx.lifecycle.viewModelScope
import com.example.hellocowcow.app.module.BaseViewModel
import com.example.hellocowcow.core.wallet.WalletClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
  private val walletClient: WalletClient
) : BaseViewModel() {

  fun handleExistingSession(
    onSessionAvailable: (String, String) -> Unit,
    onNoSession: () -> Unit
  ) {
    viewModelScope.launch {
      runCatching {
        walletClient.getActiveSession()
      }.onSuccess { session ->
        if (session != null) {
          onSessionAvailable(session.address, session.topic)
        } else {
          onNoSession()
        }
      }.onFailure { error ->
        Timber.tag("Session").e(error, "Unable to read active wallet session")
        onNoSession()
      }
    }
  }

  fun connectToWallet(
    pairingTopicPosition: Int = -1,
    onProposedSequence: (String) -> Unit = {}
  ) {
    walletClient.connect(
      pairingTopicPosition = pairingTopicPosition,
      sessionExpiryEpochSeconds = getSessionExpiryEpochSeconds(),
      onSuccess = onProposedSequence,
      onError = { message ->
        Timber.tag("WalletConnect").e(message)
      }
    )
  }

  private fun getSessionExpiryEpochSeconds(): Long =
    (System.currentTimeMillis() / 1000) +
        TimeUnit.SECONDS.convert(7, TimeUnit.DAYS)
}
