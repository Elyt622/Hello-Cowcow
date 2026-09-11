package com.example.hellocowcow.ui.screen.profile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    is WalletViewModel.UiState.Success -> NftGrid(
      title = "${state.data.size} CowCows owned",
      nfts = state.data,
      onNftClick = onNftClick
    )

    WalletViewModel.UiState.Loading -> LoadingGrid()

    is WalletViewModel.UiState.Error -> ErrorGrid(state.error)

    WalletViewModel.UiState.NoData -> EmptyGrid("No CowCow in this wallet yet")
  }
}

@Composable
private fun NftGrid(
  title: String,
  nfts: List<com.example.hellocowcow.domain.models.DomainNft>,
  onNftClick: (String) -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Text(
      text = title,
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
      style = MaterialTheme.typography.titleMedium
    )

    LazyVerticalGrid(
      modifier = Modifier.fillMaxSize(),
      columns = GridCells.Adaptive(150.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(nfts, key = { it.identifier.orEmpty() }) { nft ->
        NftCard(nft = nft) {
          nft.identifier?.let(onNftClick)
        }
      }
    }
  }
}

@Composable
private fun LoadingGrid() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator()
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
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
