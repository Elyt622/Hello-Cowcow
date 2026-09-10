package com.example.hellocowcow.ui.viewmodels.activity

import com.example.hellocowcow.app.module.BaseViewModel
import com.example.hellocowcow.ui.viewmodels.util.MyWalletConnect
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
  private val wc: MyWalletConnect
) : BaseViewModel() {

  var address: String = ""

  fun handleExistingSession(
    onSessionAvailable: (String, String) -> Unit,
    onNoSession: () -> Unit
  ) {
    val activeSessions = SignClient.getListOfActiveSessions()

    if (activeSessions.isNotEmpty()) {
      val activeSession = activeSessions.first()
      val topic = activeSession.topic
      val accounts = activeSession.namespaces.values
        .flatMap { it.accounts }
        .firstOrNull()
        ?.removePrefix("mvx:1:")
        ?: ""

      if (accounts.isNotEmpty()) {
        onSessionAvailable(accounts, topic)
      } else {
        Timber.tag("Session").d("No valid accounts found in the active session.")
        onNoSession()
      }
    } else {
      Timber.tag("Session").d("No active session found.")
      onNoSession()
    }
  }

  private fun getProperties(): Map<String, String> {
    val expiry = (System.currentTimeMillis() / 1000) +
        TimeUnit.SECONDS.convert(7, TimeUnit.DAYS)
    return mapOf("sessionExpiry" to "$expiry")
  }

  fun connectToWallet(
    pairingTopicPosition: Int = -1,
    onProposedSequence: (String) -> Unit = {},
    onSessionAvailable: (String, String) -> Unit = { _, _ -> },
    onNoSession: () -> Unit = {}
  ) {
    handleExistingSession(
      onSessionAvailable = { address, topic ->
        Timber.tag("Session").d("Reusing existing session: Address=$address, Topic=$topic")
        onSessionAvailable(address, topic)
      },
      onNoSession = {
        Timber.tag("Session").d("No active session found, creating a new one.")
        createNewConnection(pairingTopicPosition, onProposedSequence)
        onNoSession()
      }
    )
  }

  private fun createNewConnection(
    pairingTopicPosition: Int,
    onProposedSequence: (String) -> Unit
  ) {
    val pairing: Core.Model.Pairing? = if (pairingTopicPosition > -1) {
      CoreClient.Pairing.getPairings().getOrNull(pairingTopicPosition)
    } else {
      CoreClient.Pairing.create { error ->
        throw IllegalStateException(
          "Creating Pairing failed: ${error.throwable.stackTraceToString()}"
        )
      }
    }

    if (pairing == null) {
      Timber.tag("ERROR").e("Failed to create or retrieve a pairing.")
      return
    }

    val connectParams = Sign.Params.Connect(
      namespaces = wc.dAppDelegate.namespaces,
      optionalNamespaces = null,
      properties = getProperties(),
      pairing = pairing
    )

    SignClient.connect(
      connectParams,
      onSuccess = {
        onProposedSequence(pairing.uri)
      },
      onError = { error ->
        Timber.tag("ERROR").e(error.throwable.stackTraceToString())
      }
    )
  }

}
