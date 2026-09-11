package com.example.hellocowcow.app.module.main.app

import android.app.Application
import android.content.pm.ApplicationInfo
import com.example.hellocowcow.R
import com.example.hellocowcow.core.wallet.ReownDAppDelegate
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {

  override fun onCreate() {
    super.onCreate()

    if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      Timber.plant(Timber.DebugTree())
    }

    val projectId = resources.getString(R.string.wallet_connect_id)
    val relayUrl = "relay.walletconnect.com"
    val serverUrl = "wss://$relayUrl?projectId=$projectId"
    val connectionType = ConnectionType.AUTOMATIC
    val appMetaData = Core.Model.AppMetaData(
      name = "Hello CowCow",
      description = "",
      url = "https://hellocowcow.io",
      icons = listOf("https://www.cowcow.io/static/media/logo_new.1bd828ac79a450fe1a9f789fd29a8793.svg"),
      redirect = getString(R.string.deep_link_url)
    )

    CoreClient.initialize(
      relayServerUrl = serverUrl,
      connectionType = connectionType,
      application = this,
      metaData = appMetaData
    ) { error ->
      Timber.tag("CoreClient_Init_Error")
        .e(error.throwable.stackTraceToString())
    }

    SignClient.initialize(
      init = Sign.Params.Init(core = CoreClient),
      onSuccess = {
        ReownDAppDelegate.register()
      },
      onError = { error ->
        Timber.tag("SignClient_Init_Error")
          .e(error.throwable)
      }
    )
  }
}
