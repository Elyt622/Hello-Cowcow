package com.example.hellocowcow.domain.models

import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import timber.log.Timber

object DAppDelegate : SignClient.DappDelegate {

  var selectedSessionTopic: String? = null
    private set

  private val chains = listOf("mvx:1")
  private val methods = listOf(
    "mvx_signTransaction",
    "mvx_signTransactions",
    "mvx_signMessage",
    "mvx_signLoginToken",
    "mvx_signNativeAuthToken"
  )
  private val events = emptyList<String>()
  val namespaces =
    mapOf("mvx" to Sign.Model.Namespace.Proposal(chains, methods, events))

  private val wcEventSubject: Subject<Sign.Model> = PublishSubject.create()
  val wcEventObservable: Observable<Sign.Model> = wcEventSubject.hide()

  init {
    SignClient.setDappDelegate(this)
    Timber.tag("DEBUG").d(SignClient.getListOfActiveSessions().toString())
  }

  override fun onSessionApproved(
    approvedSession: Sign.Model.ApprovedSession
  ) {
    selectedSessionTopic = approvedSession.topic
    wcEventSubject.onNext(approvedSession)

    Timber.tag("Session_Approved")
      .d("Approved session's topic is: %s", approvedSession.topic)
  }

  override fun onSessionRejected(
    rejectedSession: Sign.Model.RejectedSession
  ) {
    wcEventSubject.onNext(rejectedSession)
    Timber.tag("Session_Rejected")
      .d(rejectedSession.reason)
  }

  override fun onSessionUpdate(
    updatedSession: Sign.Model.UpdatedSession
  ) {
    wcEventSubject.onNext(updatedSession)
    Timber.tag("Session_Updated").d(updatedSession.toString())
  }

  override fun onSessionExtend(session: Sign.Model.Session) {
    wcEventSubject.onNext(session)
    Timber.tag("Session_Extended").d(session.toString())
  }

  @Deprecated(
    "onSessionEvent is deprecated. Use onEvent instead. Using both will result in duplicate events.",
    replaceWith = ReplaceWith("onEvent(event)")
  )
  override fun onSessionEvent(
    sessionEvent: Sign.Model.SessionEvent
  ) {
    wcEventSubject.onNext(sessionEvent)
    Timber.tag("Received_Session_Event").d(sessionEvent.toString())
  }

  override fun onSessionDelete(
    deletedSession: Sign.Model.DeletedSession
  ) {
    deselectAccountDetails()
    wcEventSubject.onNext(deletedSession)
    Timber.tag("Session_Deleted").d(deletedSession.toString())
  }

  override fun onSessionRequestResponse(
    response: Sign.Model.SessionRequestResponse
  ) {
    wcEventSubject.onNext(response)
    Timber.tag("Session_Request_Resp").d(response.toString())
  }

  override fun onConnectionStateChange(
    state: Sign.Model.ConnectionState
  ) {
    wcEventSubject.onNext(state)
    Timber.tag("Connection_State").d(state.toString())
  }

  override fun onError(
    error: Sign.Model.Error
  ) {
    wcEventSubject.onNext(error)
    Timber.tag("Error_In_SignClient_SDK").e(error.toString())
  }

  override fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) {
    wcEventSubject.onNext(proposal)
    Timber.tag("Proposal_Expired").w(proposal.toString())
  }

  override fun onRequestExpired(request: Sign.Model.ExpiredRequest) {
    wcEventSubject.onNext(request)
    Timber.tag("Request_Expired").w(request.toString())
  }

  private fun deselectAccountDetails() {
    selectedSessionTopic = null
  }

}
