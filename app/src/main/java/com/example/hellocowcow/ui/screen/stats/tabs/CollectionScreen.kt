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
import androidx.compose.ui.text.font.FontWeight
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
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      CollectionHero(collection, locale)
    }

    item { SectionLabel("The herd", "Supply and ownership") }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        MetricCard("Staked", formatCount(locale, collection.stakedCount), Modifier.weight(1f))
        MetricCard("Listed", formatCount(locale, collection.listedCount), Modifier.weight(1f))
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        MetricCard("Holders", formatCount(locale, collection.holdersCount), Modifier.weight(1f))
        MetricCard("Upgraded", formatCount(locale, collection.totalUpgradedCount), Modifier.weight(1f))
      }
    }

    item { SectionLabel("Trading", "Volume and market depth") }

    item {
      InsightCard(
        rows = listOf(
          "24h volume" to formatEgld(locale, collection.dayEgldVolume),
          "7d volume" to formatEgld(locale, collection.weekEgldVolume),
          "All-time volume" to formatEgld(locale, collection.totalEgldVolume)
        )
      )
    }

    item { SectionLabel("History", "Longer-term collection signals") }

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
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "FLOOR PRICE",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
        )
        Text(
          text = formatEgld(locale, collection.floorPrice),
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold
        )
      }
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = "ATH",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
        )
        Text(
          text = collection.athEgldPrice?.let { formatEgld(locale, it) } ?: "—",
          style = MaterialTheme.typography.titleSmall
        )
      }
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
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun InsightCard(rows: List<Pair<String, String>>) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
      rows.forEachIndexed { index, row ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = row.first,
            style = MaterialTheme.typography.bodySmall,
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
private fun SectionLabel(title: String, subtitle: String) {
  Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
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
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer
      ),
      shape = RoundedCornerShape(14.dp)
    ) {
      Text(
        text = message,
        modifier = Modifier.padding(16.dp),
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
