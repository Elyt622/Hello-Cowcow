package com.example.hellocowcow.ui.screen.profile.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hellocowcow.ui.composables.NftCard
import com.example.hellocowcow.ui.theme.Typography2
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
    is WalletViewModel.UiState.Success -> {
      Text(
        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
        text = "Cows: ${state.data.size}",
        style = MaterialTheme.typography.labelMedium
      )
      LazyVerticalGrid(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 50.dp),
        columns = GridCells.Adaptive(150.dp)
      ) {
        items(state.data) { nft ->
          NftCard(nft = nft) {
            nft.identifier?.let(onNftClick)
          }
        }
      }
    }

    WalletViewModel.UiState.Loading -> LoadingGrid()

    is WalletViewModel.UiState.Error -> ErrorGrid(state.error)

    WalletViewModel.UiState.NoData -> EmptyGrid("No NFT in the wallet")
  }
}

@Composable
private fun LoadingGrid() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator(
      modifier = Modifier.width(60.dp),
      color = MaterialTheme.colorScheme.primary
    )
  }
}

@Composable
private fun ErrorGrid(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = message,
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.bodyMedium
    )
  }
}

@Composable
private fun EmptyGrid(message: String) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = message,
      style = Typography2.bodyLarge
    )
  }
}
