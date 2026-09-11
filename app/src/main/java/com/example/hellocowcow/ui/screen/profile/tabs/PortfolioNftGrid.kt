package com.example.hellocowcow.ui.screen.profile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.hellocowcow.domain.models.DomainNft
import com.example.hellocowcow.ui.composables.NftCard

private enum class PortfolioFilter(val label: String) {
  All("All"),
  Listed("Listed"),
  Upgraded("Upgraded")
}

@Composable
internal fun PortfolioNftGrid(
  title: String,
  nfts: List<DomainNft>,
  onNftClick: (String) -> Unit
) {
  var query by rememberSaveable { mutableStateOf("") }
  var sortByRank by rememberSaveable { mutableStateOf(false) }
  var filter by rememberSaveable { mutableStateOf(PortfolioFilter.All) }
  val normalizedQuery = query.trim().lowercase()

  val visibleNfts = remember(nfts, normalizedQuery, sortByRank, filter) {
    val filteredByType = when (filter) {
      PortfolioFilter.All -> nfts
      PortfolioFilter.Listed -> nfts.filter { it.onSale == true }
      PortfolioFilter.Upgraded -> nfts.filter { it.hasSecondNFT == true }
    }

    val filteredByQuery = if (normalizedQuery.isBlank()) {
      filteredByType
    } else {
      filteredByType.filter { nft ->
        nft.name.orEmpty().lowercase().contains(normalizedQuery) ||
            nft.identifier.orEmpty().lowercase().contains(normalizedQuery)
      }
    }

    if (sortByRank) {
      filteredByQuery.sortedWith(
        compareBy<DomainNft> { it.metadata?.rarity?.rank ?: Int.MAX_VALUE }
          .thenBy { it.name.orEmpty() }
      )
    } else {
      filteredByQuery
    }
  }

  if (visibleNfts.isEmpty()) {
    Column(modifier = Modifier.fillMaxSize()) {
      PortfolioControls(
        title = title,
        query = query,
        onQueryChange = { query = it },
        filter = filter,
        onFilterChange = { filter = it },
        sortByRank = sortByRank,
        onSortByRankChange = { sortByRank = it },
        visibleCount = 0,
        normalizedQuery = normalizedQuery
      )
      EmptyPortfolioState(
        when {
          normalizedQuery.isNotBlank() -> "No CowCow matches \"$query\""
          filter == PortfolioFilter.Listed -> "No listed CowCow here"
          filter == PortfolioFilter.Upgraded -> "No upgraded CowCow here"
          else -> "No CowCow available"
        }
      )
    }
    return
  }

  LazyVerticalGrid(
    modifier = Modifier.fillMaxSize(),
    columns = GridCells.Adaptive(150.dp),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      PortfolioControls(
        title = title,
        query = query,
        onQueryChange = { query = it },
        filter = filter,
        onFilterChange = { filter = it },
        sortByRank = sortByRank,
        onSortByRankChange = { sortByRank = it },
        visibleCount = visibleNfts.size,
        normalizedQuery = normalizedQuery
      )
    }

    items(visibleNfts, key = { it.identifier.orEmpty() }) { nft ->
      NftCard(nft = nft) {
        nft.identifier?.let(onNftClick)
      }
    }
  }
}

@Composable
private fun PortfolioControls(
  title: String,
  query: String,
  onQueryChange: (String) -> Unit,
  filter: PortfolioFilter,
  onFilterChange: (PortfolioFilter) -> Unit,
  sortByRank: Boolean,
  onSortByRankChange: (Boolean) -> Unit,
  visibleCount: Int,
  normalizedQuery: String
) {
  Column(
    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium
      )
      Text(
        text = when {
          normalizedQuery.isNotBlank() -> "$visibleCount result${if (visibleCount == 1) "" else "s"}"
          filter != PortfolioFilter.All -> "$visibleCount ${filter.label.lowercase()}"
          else -> "$visibleCount visible"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      label = { Text("Find by name or identifier") },
      leadingIcon = {
        Icon(
          imageVector = Icons.Filled.Search,
          contentDescription = null
        )
      },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(onClick = { onQueryChange("") }) {
            Icon(
              imageVector = Icons.Filled.Clear,
              contentDescription = "Clear search"
            )
          }
        }
      }
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      PortfolioFilter.entries.forEach { option ->
        FilterChip(
          selected = filter == option,
          onClick = { onFilterChange(option) },
          label = { Text(option.label) }
        )
      }

      FilterChip(
        selected = sortByRank,
        onClick = { onSortByRankChange(!sortByRank) },
        label = { Text("Best rank") }
      )
    }
  }
}

@Composable
internal fun PortfolioLoadingState() {
  LazyVerticalGrid(
    modifier = Modifier.fillMaxSize(),
    columns = GridCells.Adaptive(150.dp),
    contentPadding = PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(6) {
      Column(
        modifier = Modifier
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
        )
        Column(
          modifier = Modifier.padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(0.72f)
              .height(14.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
          )
          Box(
            modifier = Modifier
              .fillMaxWidth(0.46f)
              .height(10.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
          )
        }
      }
    }
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
