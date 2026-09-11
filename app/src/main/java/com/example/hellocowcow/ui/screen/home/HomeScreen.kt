package com.example.hellocowcow.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
  val hero = if (isSystemInDarkTheme()) R.drawable.home_dark else R.drawable.home_light

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 20.dp,
      top = 20.dp,
      end = 20.dp,
      bottom = 32.dp
    ),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    item {
      ExploreHeader()
    }

    item {
      ExploreHero(
        hero = hero,
        onCollectionClick = onCollectionClick,
        onPortfolioClick = onPortfolioClick
      )
    }

    item {
      SectionTitle(
        title = "Market snapshot",
        subtitle = "A quick read on CowCow right now"
      )
    }

    when (val state = statsState) {
      HomeViewModel.UiState.Loading -> item {
        LoadingPanel()
      }

      is HomeViewModel.UiState.Error -> item {
        ErrorPanel(
          title = "Market data unavailable",
          message = state.error
        )
      }

      is HomeViewModel.UiState.Success -> item {
        MarketSnapshot(
          stats = state.data,
          locale = locale
        )
      }
    }

    item {
      SectionTitle(
        title = "Latest sales",
        subtitle = "What collectors are buying"
      )
    }

    when (val state = salesState) {
      HomeViewModel.UiStateSold.Loading -> item {
        LoadingPanel()
      }

      is HomeViewModel.UiStateSold.Error -> item {
        ErrorPanel(
          title = "Sales unavailable",
          message = state.error
        )
      }

      is HomeViewModel.UiStateSold.Success -> item {
        LatestSales(
          sales = state.data,
          locale = locale
        )
      }
    }
  }
}

@Composable
private fun ExploreHeader() {
  Column(
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = "Hello CowCow",
      style = MaterialTheme.typography.headlineLarge,
      color = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = "Your live view of the CowCow ecosystem on MultiversX.",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ExploreHero(
  hero: Int,
  onCollectionClick: () -> Unit,
  onPortfolioClick: () -> Unit
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(224.dp)
        .clip(RoundedCornerShape(28.dp))
    ) {
      GlideImage(
        model = hero,
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
                Color.Black.copy(alpha = 0.72f)
              ),
              startY = 40f
            )
          )
      )

      Surface(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(16.dp),
        shape = RoundedCornerShape(100.dp),
        color = Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White
      ) {
        Text(
          text = "LIVE · MULTIVERSX",
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold
        )
      }

      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = "Explore the herd",
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White
        )
        Text(
          text = "Market activity is public. Connect xPortal only for your portfolio and rewards.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.88f)
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = onCollectionClick,
        modifier = Modifier.weight(1f)
      ) {
        Text("View collection")
      }

      FilledTonalButton(
        onClick = onPortfolioClick,
        modifier = Modifier.weight(1f)
      ) {
        Text("My portfolio")
      }
    }
  }
}

@Composable
private fun MarketSnapshot(
  stats: HomeViewModel.SomeStats,
  locale: Locale
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
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
        supporting = "All-time volume",
        modifier = Modifier.weight(1f)
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
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
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer
      )
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
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
          verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Text(
            text = "Market cap",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
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
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor)
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor.copy(alpha = 0.72f)
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = supporting,
        style = MaterialTheme.typography.bodySmall,
        color = contentColor.copy(alpha = 0.68f)
      )
    }
  }
}

@Composable
private fun SectionTitle(
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
private fun LatestSales(
  sales: List<Resources>,
  locale: Locale
) {
  if (sales.isEmpty()) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surfaceVariant
    ) {
      Text(
        text = "No recent sales to show.",
        modifier = Modifier.padding(20.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    return
  }

  LazyRow(
    contentPadding = PaddingValues(end = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(sales) { sale ->
      SaleCard(
        sale = sale,
        locale = locale
      )
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
    modifier = Modifier.width(178.dp),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
  ) {
    GlideImage(
      model = sale.webpUrl ?: sale.url,
      contentDescription = sale.name,
      modifier = Modifier
        .fillMaxWidth()
        .height(142.dp),
      contentScale = ContentScale.Crop
    )

    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp)
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
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
      )
      Text(
        text = sale.usdPrice?.let {
          "$${formatNumber(locale, "%,.0f", it)}"
        } ?: "",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun LoadingPanel() {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .height(132.dp),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
  }
}

@Composable
private fun ErrorPanel(
  title: String,
  message: String
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer
    )
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
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

private fun formatNumber(
  locale: Locale,
  pattern: String,
  value: Double
): String = String.format(locale, pattern, value)
