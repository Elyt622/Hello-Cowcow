package com.example.hellocowcow.ui.screen.profile.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hellocowcow.ui.viewmodels.screen.profile.WalletViewModel

@Composable
fun WalletScreen(
  viewModel: WalletViewModel,
  address: String,
  onNftClick: (String) -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(address) {
    viewModel.setAddress(address)
    viewModel.getCowsInWallet()
  }

  when (val state = uiState) {
    is WalletViewModel.UiState.Success -> PortfolioNftGrid(
      title = "${state.data.size} CowCows owned",
      nfts = state.data,
      onNftClick = onNftClick
    )

    WalletViewModel.UiState.Loading -> PortfolioLoadingState()

    is WalletViewModel.UiState.Error -> PortfolioErrorState(state.error)

    WalletViewModel.UiState.NoData -> EmptyPortfolioState("No CowCow in this wallet yet")
  }
}
