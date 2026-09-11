package com.example.hellocowcow.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.hellocowcow.ui.screen.home.HomeScreen
import com.example.hellocowcow.ui.screen.portfolio.ConnectWalletScreen
import com.example.hellocowcow.ui.screen.profile.ProfileScreen
import com.example.hellocowcow.ui.screen.stats.StatsScreen
import com.example.hellocowcow.ui.viewmodels.activity.MainViewModel

private data class TopLevelItem(
  val destination: AppDestination,
  val label: String,
  val icon: ImageVector
)

private val topLevelItems = listOf(
  TopLevelItem(ExploreDestination, "Explore", Icons.Filled.Home),
  TopLevelItem(CollectionDestination, "Collection", Icons.Filled.QueryStats),
  TopLevelItem(PortfolioDestination, "Portfolio", Icons.Filled.Person)
)

@Composable
fun AppShell(
  walletState: MainViewModel.WalletUiState,
  onConnectWallet: () -> Unit,
  onRetryWallet: () -> Unit,
  onExit: () -> Unit
) {
  val backStack = rememberNavBackStack(ExploreDestination)
  val currentDestination = backStack.lastOrNull()

  fun navigateTopLevel(destination: AppDestination) {
    while (backStack.size > 1) {
      backStack.removeLastOrNull()
    }
    if (destination != ExploreDestination) {
      backStack.add(destination)
    }
  }

  Scaffold(
    bottomBar = {
      NavigationBar {
        topLevelItems.forEach { item ->
          NavigationBarItem(
            selected = currentDestination == item.destination,
            onClick = { navigateTopLevel(item.destination) },
            icon = {
              androidx.compose.material3.Icon(
                imageVector = item.icon,
                contentDescription = item.label
              )
            },
            label = { Text(item.label) }
          )
        }
      }
    }
  ) { paddingValues ->
    NavDisplay(
      backStack = backStack,
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      onBack = {
        if (backStack.size > 1) {
          backStack.removeLastOrNull()
        } else {
          onExit()
        }
      },
      entryProvider = entryProvider {
        entry<ExploreDestination> {
          HomeScreen(
            viewModel = hiltViewModel(),
            onCollectionClick = { navigateTopLevel(CollectionDestination) },
            onPortfolioClick = { navigateTopLevel(PortfolioDestination) }
          )
        }

        entry<CollectionDestination> {
          StatsScreen()
        }

        entry<PortfolioDestination> {
          PortfolioContent(
            walletState = walletState,
            onConnectWallet = onConnectWallet,
            onRetryWallet = onRetryWallet
          )
        }
      }
    )
  }
}

@Composable
private fun PortfolioContent(
  walletState: MainViewModel.WalletUiState,
  onConnectWallet: () -> Unit,
  onRetryWallet: () -> Unit
) {
  when (walletState) {
    MainViewModel.WalletUiState.CheckingSession,
    is MainViewModel.WalletUiState.LoadingAccount -> PortfolioLoading()

    MainViewModel.WalletUiState.Disconnected -> ConnectWalletScreen(
      connecting = false,
      onPrimaryAction = onConnectWallet
    )

    MainViewModel.WalletUiState.Connecting -> ConnectWalletScreen(
      connecting = true,
      onPrimaryAction = onConnectWallet
    )

    is MainViewModel.WalletUiState.Connected -> ProfileScreen(
      account = walletState.account,
      topic = walletState.topic,
      viewModel = hiltViewModel()
    )

    is MainViewModel.WalletUiState.Error -> ConnectWalletScreen(
      connecting = false,
      errorMessage = walletState.message,
      primaryLabel = "Retry",
      onPrimaryAction = onRetryWallet
    )
  }
}

@Composable
private fun PortfolioLoading() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      CircularProgressIndicator()
      Text(
        text = "Loading portfolio…",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
