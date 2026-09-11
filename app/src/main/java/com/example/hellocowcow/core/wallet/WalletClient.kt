package com.example.hellocowcow.core.wallet

import com.example.hellocowcow.core.config.CowCowConfig
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class WalletSession(
  val address: String,
  val topic: String
)

sealed interface WalletEvent {
  data object Ready : WalletEvent

  data class SessionApproved(
    val session: WalletSession
  ) : WalletEvent

  data class SessionDisconnected(
    val topic: String?
  ) : WalletEvent

  data class TransactionSignatureResult(
    val requestId: Long,
    val payload: Any?
  ) : WalletEvent

  data class TransactionSignatureError(
    val requestId: Long,
    val message: String
  ) : WalletEvent

  data class RequestExpired(
    val requestId: Long
  ) : WalletEvent

  data class ConnectionError(
    val message: String
  ) : WalletEvent
}

interface WalletClient {
  val events: SharedFlow<WalletEvent>

  suspend fun getActiveSession(): WalletSession?

  fun connect(
    pairingTopicPosition: Int = -1,
    sessionExpiryEpochSeconds: Long,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  )

  fun requestTransactionSignature(
    sessionTopic: String,
    paramsJson: String,
    onSent: (Long) -> Unit,
    onError: (String) -> Unit
  )
}

internal fun walletSessionFromAccounts(
  topic: String,
  accounts: Iterable<String>
): WalletSession? {
  val accountPrefix = "${CowCowConfig.MAINNET_CAIP_CHAIN_ID}:"
  val address = accounts
    .firstOrNull { it.startsWith(accountPrefix) }
    ?.removePrefix(accountPrefix)
    ?.takeIf { it.isNotBlank() }
    ?: return null

  return WalletSession(
    address = address,
    topic = topic
  )
}

@Singleton
class ReownWalletClient @Inject constructor() : WalletClient {

  override val events: SharedFlow<WalletEvent> = ReownDAppDelegate.events

  override suspend fun getActiveSession(): WalletSession? = withContext(Dispatchers.IO) {
    val session = SignClient.getListOfActiveSessions().firstOrNull()
      ?: return@withContext null

    walletSessionFromAccounts(
      topic = session.topic,
      accounts = session.namespaces.values.flatMap { it.accounts }
    )
  }

  override fun connect(
    pairingTopicPosition: Int,
    sessionExpiryEpochSeconds: Long,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    runCatching {
      val pairing: Core.Model.Pairing? = if (pairingTopicPosition > -1) {
        CoreClient.Pairing.getPairings().getOrNull(pairingTopicPosition)
      } else {
        CoreClient.Pairing.create { error ->
          onError(
            error.throwable.message ?: "Unable to create WalletConnect pairing"
          )
        }
      }

      if (pairing == null) {
        onError("Unable to create or retrieve WalletConnect pairing")
        return
      }

      val connectParams = Sign.Params.ConnectParams(
        sessionNamespaces = ReownDAppDelegate.namespaces,
        properties = mapOf("sessionExpiry" to sessionExpiryEpochSeconds.toString()),
        pairing = pairing
      )

      SignClient.connect(
        connectParams = connectParams,
        onSuccess = onSuccess,
        onError = { error ->
          onError(error.throwable.message ?: "Unable to connect to xPortal")
        }
      )
    }.onFailure { error ->
      onError(error.message ?: "WalletConnect is not ready")
    }
  }

  override fun requestTransactionSignature(
    sessionTopic: String,
    paramsJson: String,
    onSent: (Long) -> Unit,
    onError: (String) -> Unit
  ) {
    runCatching {
      SignClient.request(
        request = Sign.Params.Request(
          sessionTopic = sessionTopic,
          method = ReownDAppDelegate.MVX_SIGN_TRANSACTION_METHOD,
          chainId = CowCowConfig.MAINNET_CAIP_CHAIN_ID,
          params = paramsJson
        ),
        onSuccess = { request -> onSent(request.requestId) },
        onError = { error ->
          onError(
            error.throwable.message ?: "Unable to send signing request to xPortal"
          )
        }
      )
    }.onFailure { error ->
      onError(error.message ?: "WalletConnect is not ready")
    }
  }
}
