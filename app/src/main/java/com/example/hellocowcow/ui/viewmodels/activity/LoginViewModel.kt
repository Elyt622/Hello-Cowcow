package com.example.hellocowcow.ui.viewmodels.activity

import com.example.hellocowcow.app.module.BaseViewModel
import com.example.hellocowcow.ui.viewmodels.util.MySchedulers
import com.example.hellocowcow.ui.viewmodels.util.MyWalletConnect
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.PublishSubject
import timber.log.Timber.Forest.tag
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
  private val wc: MyWalletConnect,
  private val mySchedulers: MySchedulers
) : BaseViewModel() {

  var address: String = ""
  var topic: String = ""

  private val startActivitySubject: PublishSubject<Unit> = PublishSubject.create()

  fun login(): Observable<Unit> {
    wc.dAppDelegate
      .wcEventObservable
      .subscribeOn(mySchedulers.io)
      .observeOn(mySchedulers.main)
      .subscribeBy(
        onNext = { session ->
          when (session) {
            is Sign.Model.ApprovedSession -> {
              tag("Approve").d(topic)
              topic = session.topic
              address = session.accounts[0].removePrefix("mvx:1:")
              startActivitySubject.onNext(Unit)
            }

            is Sign.Model.RejectedSession -> {
              tag("Rejected").d("Rejected")
            }

            else -> {
              session.toString()
            }
          }
        },
        onError = { err ->
          tag("Subscribe_Error").d(err)
        }
      ).addTo(disposable)
    return startActivitySubject
  }


  private fun getProperties(): Map<String, String> {
    val expiry = (System.currentTimeMillis() / 1000) + TimeUnit.SECONDS.convert(7, TimeUnit.DAYS)
    return mapOf("sessionExpiry" to "$expiry")
  }

  fun connectToWallet(
    pairingTopicPosition: Int = -1,
    onProposedSequence: (String) -> Unit = {}
  ) {
    val pairing: Core.Model.Pairing? = if (pairingTopicPosition > -1) {
      CoreClient.Pairing.getPairings()[pairingTopicPosition]
    } else {
      CoreClient.Pairing.create { error ->
        throw IllegalStateException("Creating Pairing failed: ${error.throwable.stackTraceToString()}")
      }
    }

    val connectParams =
      Sign.Params.Connect(
        namespaces = wc.dAppDelegate.namespaces,
        optionalNamespaces = null,
        properties = getProperties(),
        pairing = pairing!!
      )

    // Todo pairing must be not null
    SignClient.connect(connectParams,
      onSuccess = {
        onProposedSequence(pairing.uri)
      },
      onError = { error ->
        tag("ERROR").e(error.throwable.stackTraceToString())
      }
    )
  }

}