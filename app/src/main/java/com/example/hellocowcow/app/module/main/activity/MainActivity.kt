package com.example.hellocowcow.app.module.main.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hellocowcow.ui.navigation.AppShell
import com.example.hellocowcow.ui.theme.HelloCowCowTheme
import com.example.hellocowcow.ui.viewmodels.activity.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private val viewModel by viewModels<MainViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    setContent {
      val walletState by viewModel.walletState.collectAsStateWithLifecycle()

      LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
          when (effect) {
            is MainViewModel.UiEffect.OpenXPortal -> openXPortal(effect.pairingUri)
          }
        }
      }

      HelloCowCowTheme(dynamicColor = false) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          AppShell(
            walletState = walletState,
            onConnectWallet = viewModel::connectWallet,
            onRetryWallet = viewModel::refreshWalletSession,
            onExit = { finish() }
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshWalletSession()
  }

  private fun openXPortal(pairingUri: String) {
    val deepLink = Uri.parse("https://maiar.page.link/")
      .buildUpon()
      .appendQueryParameter("apn", "com.multiversx.maiar.wallet")
      .appendQueryParameter("isi", "1519405832")
      .appendQueryParameter("ibi", "com.multiversx.maiar.wallet")
      .appendQueryParameter(
        "link",
        "https://maiar.com/?wallet-connect=$pairingUri"
      )
      .build()

    startActivity(Intent(Intent.ACTION_VIEW, deepLink))
  }
}
