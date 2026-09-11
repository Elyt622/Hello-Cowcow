package com.example.hellocowcow.ui.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.hellocowcow.ui.viewmodels.AppViewModel

@Composable
fun MainScaffold(
  sessionState: AppViewModel.UiState,
  onConnectWallet: () -> Unit,
  onContinueAsGuest: () -> Unit
) {
  val navController = rememberNavController()

  Scaffold(
    bottomBar = { BottomBar(navController = navController) }
  ) { innerPadding ->
    HostController(
      navHostController = navController,
      sessionState = sessionState,
      onConnectWallet = onConnectWallet,
      onContinueAsGuest = onContinueAsGuest,
      modifier = Modifier.padding(innerPadding)
    )
  }
}
