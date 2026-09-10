package com.example.hellocowcow.ui.screen.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.hellocowcow.R
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Resources
import com.example.hellocowcow.domain.models.ItemNav
import com.example.hellocowcow.ui.viewmodels.screen.home.HomeViewModel
import java.util.Locale

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HomeScreen(
  navController: NavController,
  viewModel: HomeViewModel
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val uiStateSold by viewModel.uiStateSold.collectAsStateWithLifecycle()
  val locale = LocalLocale.current.platformLocale
  val hero = if (isSystemInDarkTheme()) R.drawable.home_dark else R.drawable.home_light

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      HomeHeader(
        onStatsClick = { navController.navigate(ItemNav.Stats.route) },
        onProfileClick = { navController.navigate(ItemNav.Profile.route) }
      )
    }

    item {
      GlideImage(
        model = hero,
        contentDescription = "CowCow collection",
        modifier = Modifier
          .fillMaxWidth()
          .height(190.dp)
          .clip(RoundedCornerShape(24.dp)),
        contentScale = ContentScale.Crop
      )
    }

    when (val state = uiState) {
      HomeViewModel.UiState.Loading -> item {
        LoadingCard()
      }

      is HomeViewModel.UiState.Error -> item {
        MessageCard(
          title = "Unable to load CowCow stats",
          message = state.error
        )
      }

      is HomeViewModel.UiState.Success -> item {
        StatisticsSection(
          stats = state.data,
          locale = locale
        )
      }
    }

    item {
      Text(
        text = "Latest sales",
        style = MaterialTheme.typography.titleLarge
      )
    }

    when (val state = uiStateSold) {
      HomeViewModel.UiStateSold.Loading -> item {
        LoadingCard()
      }

      is HomeViewModel.UiStateSold.Error -> item {
        MessageCard(
          title = "Sales unavailable",
          message = state.error
        )
      }

      is HomeViewModel.UiStateSold.Success -> {
        items(state.data) { sale ->
          SaleCard(sale)
        }
      }
    }
  }
}

@Composable
private fun HomeHeader(
  onStatsClick: () -> Unit,
  onProfileClick: () -> Unit
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Column {
      Text(
        text = "HELLO COWCOW",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
      )
      Text(
        text = "Your CowCow dashboard",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = onStatsClick,
        modifier = Modifier.weight(1f)
      ) {
        Text("Collection")
      }
      Button(
        onClick = onProfileClick,
        modifier = Modifier.weight(1f)
      ) {
        Text("Portfolio")
      }
    }
  }
}

@Composable
private fun StatisticsSection(
  stats: HomeViewModel.SomeStats,
  locale: Locale
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "Collection overview",
      style = MaterialTheme.typography.titleLarge
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      StatCard(
        label = "MOOVE",
        value = formatNumber(locale, "%.3f", stats.moovePrice) + " $",
        modifier = Modifier.weight(1f)
      )
      StatCard(
        label = "Market cap",
        value = formatNumber(locale, "%.0f", stats.mooveMC) + " $",
        modifier = Modifier.weight(1f)
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      StatCard(
        label = "Staked",
        value = stats.stakedCount,
        modifier = Modifier.weight(1f)
      )
      StatCard(
        label = "Listed",
        value = stats.listedCount,
        modifier = Modifier.weight(1f)
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      StatCard(
        label = "Floor",
        value = "${stats.floorPrice} EGLD",
        modifier = Modifier.weight(1f)
      )
      StatCard(
        label = "Total volume",
        value = formatNumber(locale, "%.2f", stats.totalEgldVolume) + " EGLD",
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun StatCard(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun SaleCard(sale: Resources) {
  Card(
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      GlideImage(
        model = sale.webpUrl,
        contentDescription = sale.name,
        modifier = Modifier
          .size(64.dp)
          .clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Crop
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = sale.name.toString(),
          style = MaterialTheme.typography.titleSmall
        )
        Text(
          text = "Rank #${sale.rank}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "${sale.sellerUsername ?: "Unknown"} → ${sale.buyerUsername ?: "Unknown"}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Column(
        horizontalAlignment = Alignment.End
      ) {
        Text(
          text = "${sale.egldValue} EGLD",
          style = MaterialTheme.typography.titleSmall
        )
        Text(
          text = "$${sale.usdPrice}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun LoadingCard() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 24.dp),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun MessageCard(
  title: String,
  message: String
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
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
