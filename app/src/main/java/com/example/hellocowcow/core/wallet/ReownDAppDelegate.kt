package com.example.hellocowcow.core.wallet

import com.example.hellocowcow.core.config.CowCowConfig
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

internal object ReownDAppDelegate : SignClient.DappDelegate {

  const val MVX_SIGN_TRANSACTION_METHOD = "mvx_signTransaction"

  private val methods = listOf(
    MVX_SIGN_TRANSACTION_METHOD,
    "mvx_signTransactions",
    "mvx_signMessage",
    "mvx_signLoginToken",
    "mvx_signNativeAuthToken"
  )

  val namespaces = mapOf(
    "mvx" to Sign.Model.Namespace.Proposal(
      chains = listOf(CowCowConfig.MAINNET_CAIP_CHAIN_ID),
      methods = methods,
      events = emptyList()
    )
  )

  private val _events = MutableSharedFlow<WalletEvent>(extraBufferCapacity = 32)
  val events: SharedFlow<WalletEvent> = _events.asSharedFlow()

  fun register() {
    SignClient.setDappDelegate(this)
    emit(WalletEvent.Ready)
  }

  override fun onSessionApproved(
    approvedSession: Sign.Model.ApprovedSession
  ) {
    val session = walletSessionFromAccounts(
      topic = approvedSession.topic,
      accounts = approvedSession.accounts
    )

    if (session != null) {
      emit(WalletEvent.SessionApproved(session))
    } else {
      emit(WalletEvent.ConnectionError("xPortal session does not contain a MultiversX mainnet account"))
    }

    Timber.tag("Session_Approved")
      .d("Approved session's topic is: %s", approvedSession.topic)
  }

  override fun onSessionRejected(
    rejectedSession: Sign.Model.RejectedSession
  ) {
    emit(WalletEvent.ConnectionError("xPortal rejected the connection: ${rejectedSession.reason}"))
    Timber.tag("Session_Rejected").d(rejectedSession.reason)
  }

  override fun onSessionUpdate(
    updatedSession: Sign.Model.UpdatedSession
  ) {
    walletSessionFromAccounts(
      topic = updatedSession.topic,
      accounts = updatedSession.namespaces.values.flatMap { it.accounts }
    )?.let { session ->
      emit(WalletEvent.SessionApproved(session))
    }
    Timber.tag("Session_Updated").d(updatedSession.toString())
  }

  override fun onSessionExtend(session: Sign.Model.Session) {
    Timber.tag("Session_Extended").d(session.topic)
  }

  @Deprecated(
    "onSessionEvent is deprecated. Use the Event overload instead.",
    replaceWith = ReplaceWith("onSessionEvent(sessionEvent)")
  )
  override fun onSessionEvent(
    sessionEvent: Sign.Model.SessionEvent
  ) {
    Timber.tag("Session_Event_Legacy").d(sessionEvent.toString())
  }

  override fun onSessionEvent(
    sessionEvent: Sign.Model.Event
  ) {
    Timber.tag("Session_Event").d(sessionEvent.toString())
  }

  override fun onSessionDelete(
    deletedSession: Sign.Model.DeletedSession
  ) {
    when (deletedSession) {
      is Sign.Model.DeletedSession.Success -> {
        emit(WalletEvent.SessionDisconnected(deletedSession.topic))
      }

      is Sign.Model.DeletedSession.Error -> {
        emit(
          WalletEvent.ConnectionError(
            deletedSession.error.message ?: "WalletConnect session was deleted"
          )
        )
      }
    }
    Timber.tag("Session_Deleted").d(deletedSession.toString())
  }

  override fun onSessionRequestResponse(
    response: Sign.Model.SessionRequestResponse
  ) {
    if (response.method != MVX_SIGN_TRANSACTION_METHOD) {
      Timber.tag("Session_Request_Resp")
        .d("Ignoring response for method %s", response.method)
      return
    }

    when (val result = response.result) {
      is Sign.Model.JsonRpcResponse.JsonRpcResult -> emit(
        WalletEvent.TransactionSignatureResult(
          requestId = result.id,
          payload = result.result
        )
      )

      is Sign.Model.JsonRpcResponse.JsonRpcError -> emit(
        WalletEvent.TransactionSignatureError(
          requestId = result.id,
          message = result.message
        )
      )
    }
  }

  override fun onConnectionStateChange(
    state: Sign.Model.ConnectionState
  ) {
    Timber.tag("Connection_State").d(state.toString())
  }

  override fun onError(
    error: Sign.Model.Error
  ) {
    emit(
      WalletEvent.ConnectionError(
        error.throwable.message ?: "WalletConnect error"
      )
    )
    Timber.tag("Error_In_SignClient_SDK").e(error.throwable)
  }

  override fun onProposalExpired(
    proposal: Sign.Model.ExpiredProposal
  ) {
    Timber.tag("Proposal_Expired").w(proposal.toString())
  }

  override fun onRequestExpired(
    request: Sign.Model.ExpiredRequest
  ) {
    emit(WalletEvent.RequestExpired(request.id))
    Timber.tag("Request_Expired").w(request.toString())
  }

  private fun emit(event: WalletEvent) {
    if (!_events.tryEmit(event)) {
      Timber.tag("Wallet_Event").w("Unable to emit wallet event: %s", event)
    }
  }
}
