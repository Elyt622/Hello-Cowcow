package com.example.hellocowcow.ui.screen.stats.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hellocowcow.domain.models.CowCollection
import com.example.hellocowcow.ui.viewmodels.screen.stats.CollectionViewModel
import java.util.Locale

@Composable
fun CollectionScreen(
  collectionViewModel: CollectionViewModel
) {
  val state by collectionViewModel.uiState.collectAsStateWithLifecycle()
  val locale = LocalLocale.current.platformLocale

  when (val current = state) {
    CollectionViewModel.UiState.Loading -> CollectionLoading()
    is CollectionViewModel.UiState.Error -> CollectionError(current.error)
    is CollectionViewModel.UiState.Success -> CollectionDashboard(
      collection = current.data,
      locale = locale
    )
  }
}

@Composable
private fun CollectionDashboard(
  collection: CowCollection,
  locale: Locale
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      CollectionHero(
        collection = collection,
        locale = locale
      )
    }

    item {
      SectionLabel(
        title = "The herd",
        subtitle = "Ownership and supply signals"
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        MetricCard(
          label = "Staked",
          value = formatCount(locale, collection.stakedCount),
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          label = "Listed",
          value = formatCount(locale, collection.listedCount),
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        MetricCard(
          label = "Holders",
          value = formatCount(locale, collection.holdersCount),
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          label = "Upgraded",
          value = formatCount(locale, collection.totalUpgradedCount),
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      SectionLabel(
        title = "Trading activity",
        subtitle = "Volume without invented charts"
      )
    }

    item {
      InsightCard(
        rows = listOf(
          "24h volume" to formatEgld(locale, collection.dayEgldVolume),
          "7d volume" to formatEgld(locale, collection.weekEgldVolume),
          "All-time volume" to formatEgld(locale, collection.totalEgldVolume)
        )
      )
    }

    item {
      SectionLabel(
        title = "Market history",
        subtitle = "Longer-term collection signals"
      )
    }

    item {
      InsightCard(
        rows = listOf(
          "All-time high" to formatEgld(locale, collection.athEgldPrice),
          "Total trades" to formatCount(locale, collection.totalTrades),
          "Followers" to formatCount(locale, collection.followAccountsCount)
        )
      )
    }
  }
}

@Composable
private fun CollectionHero(
  collection: CowCollection,
  locale: Locale
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
  ) {
    Column(
      modifier = Modifier.padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(
        text = "CowCow floor",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
      )
      Text(
        text = formatEgld(locale, collection.floorPrice),
        style = MaterialTheme.typography.headlineMedium
      )
      Text(
        text = collection.athEgldPrice?.let {
          "ATH ${formatEgld(locale, it)}"
        } ?: "ATH unavailable",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
      )
    }
  }
}

@Composable
private fun MetricCard(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun InsightCard(
  rows: List<Pair<String, String>>
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
      rows.forEachIndexed { index, row ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = row.first,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = row.second,
            style = MaterialTheme.typography.titleSmall
          )
        }

        if (index != rows.lastIndex) {
          androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
          )
        }
      }
    }
  }
}

@Composable
private fun SectionLabel(
  title: String,
  subtitle: String
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun CollectionLoading() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun CollectionError(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(20.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer
      ),
      shape = RoundedCornerShape(22.dp)
    ) {
      Text(
        text = message,
        modifier = Modifier.padding(18.dp),
        color = MaterialTheme.colorScheme.onErrorContainer
      )
    }
  }
}

private fun formatCount(
  locale: Locale,
  value: Int?
): String = value?.let {
  String.format(locale, "%,d", it)
} ?: "—"

private fun formatEgld(
  locale: Locale,
  value: Double?
): String = value?.let {
  "${String.format(locale, "%,.2f", it)} EGLD"
} ?: "— EGLD"
