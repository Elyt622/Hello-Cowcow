package com.example.hellocowcow.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.hellocowcow.ui.navigation.AppDestination
import com.example.hellocowcow.ui.screen.home.HomeScreen
import com.example.hellocowcow.ui.screen.profile.ConnectWalletScreen
import com.example.hellocowcow.ui.screen.profile.ProfileScreen
import com.example.hellocowcow.ui.screen.stats.StatsScreen
import com.example.hellocowcow.ui.viewmodels.AppViewModel

@Composable
fun HostController(
  navHostController: NavHostController,
  sessionState: AppViewModel.UiState,
  onConnectWallet: () -> Unit,
  onContinueAsGuest: () -> Unit,
  modifier: Modifier = Modifier
) {
  NavHost(
    navController = navHostController,
    startDestination = AppDestination.Explore.route,
    modifier = modifier
  ) {
    composable(AppDestination.Explore.route) {
      HomeScreen(
        viewModel = hiltViewModel(),
        onStatsClick = { navHostController.navigate(AppDestination.Stats.route) },
        onProfileClick = { navHostController.navigate(AppDestination.Portfolio.route) }
      )
    }

    composable(AppDestination.Stats.route) {
      StatsScreen()
    }

    composable(AppDestination.Portfolio.route) {
      when (sessionState) {
        is AppViewModel.UiState.Connected -> {
          ProfileScreen(
            account = sessionState.account,
            topic = sessionState.topic,
            viewModel = hiltViewModel()
          )
        }

        AppViewModel.UiState.Guest -> {
          ConnectWalletScreen(
            isConnecting = false,
            error = null,
            onConnect = onConnectWallet,
            onContinueAsGuest = onContinueAsGuest
          )
        }

        AppViewModel.UiState.Connecting -> {
          ConnectWalletScreen(
            isConnecting = true,
            error = null,
            onConnect = onConnectWallet,
            onContinueAsGuest = onContinueAsGuest
          )
        }

        is AppViewModel.UiState.Error -> {
          ConnectWalletScreen(
            isConnecting = false,
            error = sessionState.message,
            onConnect = onConnectWallet,
            onContinueAsGuest = onContinueAsGuest
          )
        }
      }
    }
  }
}
