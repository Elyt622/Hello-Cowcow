package com.example.hellocowcow.ui.screen.profile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.hellocowcow.domain.models.DomainNft
import com.example.hellocowcow.ui.composables.NftCard
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
    is MarketViewModel.UiState.Success -> MarketGrid(
      nfts = state.data,
      onNftClick = onNftClick
    )

    MarketViewModel.UiState.Loading -> LoadingMarketGrid()

    is MarketViewModel.UiState.Error -> ErrorMarketGrid(state.error)

    MarketViewModel.UiState.NoData -> EmptyMarketGrid("No CowCow currently listed")
  }
}

@Composable
private fun MarketGrid(
  nfts: List<DomainNft>,
  onNftClick: (String) -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Text(
      text = "${nfts.size} CowCows listed",
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
private fun LoadingMarketGrid() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun ErrorMarketGrid(message: String) {
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
private fun EmptyMarketGrid(message: String) {
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
