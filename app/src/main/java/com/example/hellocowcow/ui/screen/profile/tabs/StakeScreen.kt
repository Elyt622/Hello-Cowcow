package com.example.hellocowcow.ui.screen.profile.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hellocowcow.ui.viewmodels.screen.profile.StakeViewModel

@Composable
fun StakeScreen(
  viewModel: StakeViewModel,
  address: String,
  onNftClick: (String) -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(address) {
    viewModel.setAddress(address)
    viewModel.getAllStakingCow()
  }

  when (val state = uiState) {
    is StakeViewModel.UiState.Success -> PortfolioNftGrid(
      title = "${state.data.size} CowCows staked",
      nfts = state.data,
      onNftClick = onNftClick
    )

    StakeViewModel.UiState.Loading -> PortfolioLoadingState()

    is StakeViewModel.UiState.Error -> PortfolioErrorState(state.error)

    StakeViewModel.UiState.NoData -> EmptyPortfolioState("No CowCow currently staked")
  }
}
