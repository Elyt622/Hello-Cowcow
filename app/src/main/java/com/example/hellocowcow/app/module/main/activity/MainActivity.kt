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
import com.example.hellocowcow.ui.composables.MainScaffold
import com.example.hellocowcow.ui.theme.HelloCowCowTheme
import com.example.hellocowcow.ui.viewmodels.AppViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private val viewModel by viewModels<AppViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    setContent {
      val sessionState by viewModel.uiState.collectAsStateWithLifecycle()

      LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
          when (effect) {
            is AppViewModel.Effect.OpenXPortal -> {
              openXPortal(effect.pairingUri)
            }
          }
        }
      }

      HelloCowCowTheme(dynamicColor = false) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainScaffold(
            sessionState = sessionState,
            onConnectWallet = viewModel::connectWallet,
            onContinueAsGuest = viewModel::continueAsGuest
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshSession()
  }

  private fun openXPortal(pairingUri: String) {
    val walletLink = Uri.Builder()
      .scheme("https")
      .authority("maiar.com")
      .appendQueryParameter("wallet-connect", pairingUri)
      .build()

    val xPortalLink = Uri.Builder()
      .scheme("https")
      .authority("maiar.page.link")
      .appendQueryParameter("apn", "com.multiversx.maiar.wallet")
      .appendQueryParameter("isi", "1519405832")
      .appendQueryParameter("ibi", "com.multiversx.maiar.wallet")
      .appendQueryParameter("link", walletLink.toString())
      .build()

    startActivity(Intent(Intent.ACTION_VIEW, xPortalLink))
  }
}
