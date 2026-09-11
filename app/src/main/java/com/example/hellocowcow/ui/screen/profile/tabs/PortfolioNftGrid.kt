package com.example.hellocowcow.ui.screen.profile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hellocowcow.domain.models.DomainNft
import com.example.hellocowcow.ui.composables.NftCard

@Composable
internal fun PortfolioNftGrid(
  title: String,
  nfts: List<DomainNft>,
  onNftClick: (String) -> Unit
) {
  var query by rememberSaveable { mutableStateOf("") }
  var sortByRank by rememberSaveable { mutableStateOf(false) }
  val normalizedQuery = query.trim().lowercase()
  val visibleNfts = remember(nfts, normalizedQuery, sortByRank) {
    val filtered = if (normalizedQuery.isBlank()) {
      nfts
    } else {
      nfts.filter { nft ->
        nft.name.orEmpty().lowercase().contains(normalizedQuery) ||
            nft.identifier.orEmpty().lowercase().contains(normalizedQuery)
      }
    }

    if (sortByRank) {
      filtered.sortedWith(
        compareBy<DomainNft> { it.metadata?.rarity?.rank ?: Int.MAX_VALUE }
          .thenBy { it.name.orEmpty() }
      )
    } else {
      filtered
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium
      )

      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Find a CowCow") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null
          )
        },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "Clear search"
              )
            }
          }
        }
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (normalizedQuery.isBlank()) {
            "${visibleNfts.size} visible"
          } else {
            "${visibleNfts.size} result${if (visibleNfts.size == 1) "" else "s"}"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FilterChip(
          selected = sortByRank,
          onClick = { sortByRank = !sortByRank },
          label = { Text("Best rank") }
        )
      }
    }

    if (visibleNfts.isEmpty()) {
      EmptyPortfolioState(
        if (normalizedQuery.isBlank()) {
          "No CowCow available"
        } else {
          "No CowCow matches \"$query\""
        }
      )
      return@Column
    }

    LazyVerticalGrid(
      modifier = Modifier.fillMaxSize(),
      columns = GridCells.Adaptive(150.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(visibleNfts, key = { it.identifier.orEmpty() }) { nft ->
        NftCard(nft = nft) {
          nft.identifier?.let(onNftClick)
        }
      }
    }
  }
}

@Composable
internal fun PortfolioLoadingState() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator()
  }
}

@Composable
internal fun PortfolioErrorState(message: String) {
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
internal fun EmptyPortfolioState(message: String) {
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
