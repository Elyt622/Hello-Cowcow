package com.example.hellocowcow.app.module.main.app

import android.app.Application
import com.example.hellocowcow.R
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.Forest.plant
import timber.log.Timber.Forest.tag

@HiltAndroidApp
class App : Application() {

  override fun onCreate() {
    super.onCreate()
    plant(Timber.DebugTree())

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
      tag("CoreClient_Init_Error")
        .e(error.throwable.stackTraceToString())
    }
    val init = Sign.Params.Init(core = CoreClient)

    SignClient.initialize(init) { error ->
      tag("SignClient_Init_Error")
        .e(error.toString())
    }
  }

}