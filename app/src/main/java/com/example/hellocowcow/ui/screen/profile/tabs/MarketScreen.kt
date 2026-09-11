package com.example.hellocowcow.ui.screen.profile.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hellocowcow.ui.viewmodels.screen.profile.MarketViewModel

@Composable
fun MarketScreen(
  viewModel: MarketViewModel,
  address: String,
  onNftClick: (String) -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(address) {
    viewModel.setAddress(address)
    viewModel.getCowsListing()
  }

  when (val state = uiState) {
    is MarketViewModel.UiState.Success -> PortfolioNftGrid(
      title = "${state.data.size} CowCows listed",
      nfts = state.data,
      onNftClick = onNftClick
    )

    MarketViewModel.UiState.Loading -> PortfolioLoadingState()

    is MarketViewModel.UiState.Error -> PortfolioErrorState(state.error)

    MarketViewModel.UiState.NoData -> EmptyPortfolioState("No CowCow currently listed")
  }
}
