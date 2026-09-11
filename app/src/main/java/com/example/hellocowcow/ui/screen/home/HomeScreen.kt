package com.example.hellocowcow.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.hellocowcow.R
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Resources
import com.example.hellocowcow.ui.viewmodels.screen.home.HomeViewModel
import java.util.Locale

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  onCollectionClick: () -> Unit,
  onPortfolioClick: () -> Unit
) {
  val statsState by viewModel.uiState.collectAsStateWithLifecycle()
  val salesState by viewModel.uiStateSold.collectAsStateWithLifecycle()
  val locale = LocalLocale.current.platformLocale

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 16.dp,
      top = 16.dp,
      end = 16.dp,
      bottom = 24.dp
    ),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    item { ExploreHeader() }

    item {
      ExploreHero(
        onCollectionClick = onCollectionClick,
        onPortfolioClick = onPortfolioClick
      )
    }

    item {
      SectionTitle(
        title = "Market snapshot",
        subtitle = "CowCow right now"
      )
    }

    when (val state = statsState) {
      HomeViewModel.UiState.Loading -> item { LoadingPanel() }
      is HomeViewModel.UiState.Error -> item {
        ErrorPanel("Market data unavailable", state.error)
      }
      is HomeViewModel.UiState.Success -> item {
        MarketSnapshot(state.data, locale)
      }
    }

    item {
      SectionTitle(
        title = "Recent activity",
        subtitle = "Latest CowCow sales"
      )
    }

    when (val state = salesState) {
      HomeViewModel.UiStateSold.Loading -> item { LoadingPanel() }
      is HomeViewModel.UiStateSold.Error -> item {
        ErrorPanel("Sales unavailable", state.error)
      }
      is HomeViewModel.UiStateSold.Success -> item {
        LatestSales(state.data, locale)
      }
    }
  }
}

@Composable
private fun ExploreHeader() {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = "Hello CowCow",
      style = MaterialTheme.typography.headlineLarge,
      color = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = "Cows. MOOVE. Community. On MultiversX.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ExploreHero(
  onCollectionClick: () -> Unit,
  onPortfolioClick: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(192.dp)
        .clip(RoundedCornerShape(18.dp))
    ) {
      GlideImage(
        model = R.drawable.home_dark,
        contentDescription = "CowCow collection artwork",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
      )

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.82f)
              ),
              startY = 20f
            )
          )
      )

      Surface(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(12.dp),
        shape = RoundedCornerShape(100.dp),
        color = Color.Black.copy(alpha = 0.64f),
        contentColor = Color.White
      ) {
        Text(
          text = "LIVE · MULTIVERSX",
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold
        )
      }

      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = "Explore the herd",
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White
        )
        Text(
          text = "Public market data. Wallet only when you need it.",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.82f)
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onCollectionClick,
        modifier = Modifier.weight(1f)
      ) {
        Text("Collection")
      }
      FilledTonalButton(
        onClick = onPortfolioClick,
        modifier = Modifier.weight(1f)
      ) {
        Text("Portfolio")
      }
    }
  }
}

@Composable
private fun MarketSnapshot(
  stats: HomeViewModel.SomeStats,
  locale: Locale
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      MetricCard(
        label = "Floor",
        value = "${stats.floorPrice} EGLD",
        supporting = "Collection floor",
        highlighted = true,
        modifier = Modifier.weight(1f)
      )
      MetricCard(
        label = "Volume",
        value = "${formatNumber(locale, "%.1f", stats.totalEgldVolume)} EGLD",
        supporting = "All-time",
        modifier = Modifier.weight(1f)
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      MetricCard(
        label = "Staked",
        value = stats.stakedCount,
        supporting = "CowCows earning",
        modifier = Modifier.weight(1f)
      )
      MetricCard(
        label = "Listed",
        value = stats.listedCount,
        supporting = "Available now",
        modifier = Modifier.weight(1f)
      )
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer
      )
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
            text = "MOOVE",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
          Text(
            text = "$${formatNumber(locale, "%.4f", stats.moovePrice)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = "Market cap",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.70f)
          )
          Text(
            text = "$${formatNumber(locale, "%,.0f", stats.mooveMC)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
      }
    }
  }
}

@Composable
private fun MetricCard(
  label: String,
  value: String,
  supporting: String,
  modifier: Modifier = Modifier,
  highlighted: Boolean = false
) {
  val containerColor = if (highlighted) {
    MaterialTheme.colorScheme.primaryContainer
  } else {
    MaterialTheme.colorScheme.surfaceVariant
  }
  val contentColor = if (highlighted) {
    MaterialTheme.colorScheme.onPrimaryContainer
  } else {
    MaterialTheme.colorScheme.onSurface
  }

  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor)
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor.copy(alpha = 0.72f)
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = supporting,
        style = MaterialTheme.typography.bodySmall,
        color = contentColor.copy(alpha = 0.64f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
  Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
    Text(text = title, style = MaterialTheme.typography.titleLarge)
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun LatestSales(
  sales: List<Resources>,
  locale: Locale
) {
  if (sales.isEmpty()) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surfaceVariant
    ) {
      Text(
        text = "No recent sales to show.",
        modifier = Modifier.padding(16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    return
  }

  LazyRow(
    contentPadding = PaddingValues(end = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    items(sales) { sale ->
      SaleCard(sale, locale)
    }
  }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun SaleCard(
  sale: Resources,
  locale: Locale
) {
  Card(
    modifier = Modifier.width(158.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
  ) {
    GlideImage(
      model = sale.webpUrl ?: sale.url,
      contentDescription = sale.name,
      modifier = Modifier
        .fillMaxWidth()
        .height(124.dp),
      contentScale = ContentScale.Crop
    )

    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = sale.name ?: sale.identifier ?: "CowCow",
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = sale.rank?.let { "Rank #$it" } ?: "CowCow",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = sale.egldValue?.let {
          "${formatNumber(locale, "%.2f", it)} EGLD"
        } ?: "— EGLD",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}

@Composable
private fun LoadingPanel() {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .height(112.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(modifier = Modifier.size(26.dp))
    }
  }
}

@Composable
private fun ErrorPanel(title: String, message: String) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onErrorContainer
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer
      )
    }
  }
}

private fun formatNumber(
  locale: Locale,
  pattern: String,
  value: Double?
): String = value?.let {
  String.format(locale, pattern, it)
} ?: "—"
