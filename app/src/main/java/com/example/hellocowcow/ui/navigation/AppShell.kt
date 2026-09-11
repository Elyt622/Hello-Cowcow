package com.example.hellocowcow.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.hellocowcow.ui.screen.home.HomeScreen
import com.example.hellocowcow.ui.screen.nft.NftScreen
import com.example.hellocowcow.ui.screen.portfolio.ConnectWalletScreen
import com.example.hellocowcow.ui.screen.profile.ProfileScreen
import com.example.hellocowcow.ui.screen.recovery.RecoveryScreen
import com.example.hellocowcow.ui.screen.stats.StatsScreen
import com.example.hellocowcow.ui.viewmodels.activity.MainViewModel
import com.example.hellocowcow.ui.viewmodels.screen.nft.NftViewModel
import com.example.hellocowcow.ui.viewmodels.screen.recovery.RecoveryViewModel

private const val EXPANDED_NAVIGATION_BREAKPOINT_DP = 840

private data class TopLevelItem(
  val destination: AppDestination,
  val label: String,
  val icon: ImageVector
)

private val topLevelItems = listOf(
  TopLevelItem(ExploreDestination, "Explore", Icons.Filled.Home),
  TopLevelItem(CollectionDestination, "Collection", Icons.Filled.QueryStats),
  TopLevelItem(PortfolioDestination, "Portfolio", Icons.Filled.Person),
  TopLevelItem(RecoveryDestination, "Recovery", Icons.Filled.Build)
)

private val topLevelDestinations = topLevelItems.map { it.destination }.toSet()

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

  fun openNft(identifier: String) {
    if (identifier.isNotBlank()) {
      backStack.add(NftDetailDestination(identifier))
    }
  }

  val navContent: @Composable (Modifier) -> Unit = { modifier ->
    NavDisplay(
      backStack = backStack,
      modifier = modifier,
      onBack = {
        if (backStack.size > 1) {
          backStack.removeLastOrNull()
        } else {
          onExit()
        }
      },
      entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
      ),
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
            onRetryWallet = onRetryWallet,
            onNftClick = ::openNft
          )
        }

        entry<RecoveryDestination> {
          RecoveryContent(
            walletState = walletState,
            onConnectWallet = onConnectWallet,
            onRetryWallet = onRetryWallet
          )
        }

        entry<NftDetailDestination> { destination ->
          NftScreen(
            identifier = destination.identifier,
            viewModel = hiltViewModel<NftViewModel>(),
            onBack = { backStack.removeLastOrNull() }
          )
        }
      }
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= EXPANDED_NAVIGATION_BREAKPOINT_DP.dp &&
        currentDestination in topLevelDestinations

    if (useNavigationRail) {
      Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail {
          topLevelItems.forEach { item ->
            NavigationRailItem(
              selected = currentDestination == item.destination,
              onClick = { navigateTopLevel(item.destination) },
              icon = {
                Icon(
                  imageVector = item.icon,
                  contentDescription = item.label
                )
              },
              label = { Text(item.label) }
            )
          }
        }

        navContent(
          Modifier
            .weight(1f)
            .fillMaxSize()
        )
      }
    } else {
      Scaffold(
        bottomBar = {
          if (currentDestination in topLevelDestinations) {
            NavigationBar {
              topLevelItems.forEach { item ->
                NavigationBarItem(
                  selected = currentDestination == item.destination,
                  onClick = { navigateTopLevel(item.destination) },
                  icon = {
                    Icon(
                      imageVector = item.icon,
                      contentDescription = item.label
                    )
                  },
                  label = { Text(item.label) }
                )
              }
            }
          }
        }
      ) { paddingValues ->
        navContent(
          Modifier
            .fillMaxSize()
            .padding(paddingValues)
        )
      }
    }
  }
}

@Composable
private fun PortfolioContent(
  walletState: MainViewModel.WalletUiState,
  onConnectWallet: () -> Unit,
  onRetryWallet: () -> Unit,
  onNftClick: (String) -> Unit
) {
  when (walletState) {
    MainViewModel.WalletUiState.CheckingSession,
    is MainViewModel.WalletUiState.LoadingAccount -> WalletGateLoading("Loading portfolio…")

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
      viewModel = hiltViewModel(),
      onNftClick = onNftClick
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
private fun RecoveryContent(
  walletState: MainViewModel.WalletUiState,
  onConnectWallet: () -> Unit,
  onRetryWallet: () -> Unit
) {
  when (walletState) {
    MainViewModel.WalletUiState.CheckingSession,
    is MainViewModel.WalletUiState.LoadingAccount -> WalletGateLoading("Loading recovery tools…")

    MainViewModel.WalletUiState.Disconnected -> ConnectWalletScreen(
      connecting = false,
      primaryLabel = "Connect xPortal for recovery",
      onPrimaryAction = onConnectWallet
    )

    MainViewModel.WalletUiState.Connecting -> ConnectWalletScreen(
      connecting = true,
      primaryLabel = "Connect xPortal for recovery",
      onPrimaryAction = onConnectWallet
    )

    is MainViewModel.WalletUiState.Connected -> RecoveryScreen(
      account = walletState.account,
      topic = walletState.topic,
      viewModel = hiltViewModel<RecoveryViewModel>()
    )

    is MainViewModel.WalletUiState.Error -> ConnectWalletScreen(
      connecting = false,
      errorMessage = walletState.message,
      primaryLabel = "Retry recovery connection",
      onPrimaryAction = onRetryWallet
    )
  }
}

@Composable
private fun WalletGateLoading(message: String) {
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
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
