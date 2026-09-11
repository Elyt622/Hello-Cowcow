package com.example.hellocowcow.ui.screen.stats.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hellocowcow.R
import com.example.hellocowcow.domain.models.DomainReward
import com.example.hellocowcow.domain.models.DomainToken
import com.example.hellocowcow.ui.viewmodels.screen.stats.TokenViewModel
import java.util.Locale

@Composable
fun TokenScreen(
  viewModel: TokenViewModel
) {
  val tokenState by viewModel.uiStateT.collectAsStateWithLifecycle()
  val rewardState by viewModel.uiStateR.collectAsStateWithLifecycle()
  val locale = LocalLocale.current.platformLocale

  when (val state = tokenState) {
    TokenViewModel.UiStateToken.Loading -> TokenLoading()
    is TokenViewModel.UiStateToken.Error -> TokenError(state.error)
    is TokenViewModel.UiStateToken.Success -> TokenDashboard(
      token = state.data,
      rewardState = rewardState,
      locale = locale
    )
  }
}

@Composable
private fun TokenDashboard(
  token: DomainToken,
  rewardState: TokenViewModel.UiStateReward,
  locale: Locale
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      TokenHero(
        token = token,
        locale = locale
      )
    }

    item {
      SectionLabel(
        title = "Token network",
        subtitle = "Supply and usage signals"
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        TokenMetricCard(
          label = "Holders",
          value = formatCount(locale, token.accounts),
          modifier = Modifier.weight(1f)
        )
        TokenMetricCard(
          label = "Transactions",
          value = formatCount(locale, token.transactions),
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      TokenMetricCard(
        label = "Circulating supply",
        value = token.circulatingSupply ?: "—",
        supporting = "Excludes rewards not yet claimed",
        modifier = Modifier.fillMaxWidth()
      )
    }

    item {
      SectionLabel(
        title = "Rewards",
        subtitle = "MOOVE still waiting to be claimed"
      )
    }

    item {
      RewardCard(rewardState)
    }
  }
}

@Composable
private fun TokenHero(
  token: DomainToken,
  locale: Locale
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
  ) {
    Row(
      modifier = Modifier.padding(22.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
      ) {
        Image(
          imageVector = ImageVector.vectorResource(id = R.drawable.moovelogo),
          contentDescription = "MOOVE",
          modifier = Modifier
            .padding(12.dp)
            .size(38.dp)
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        Text(
          text = "MOOVE",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
        )
        Text(
          text = token.price?.let {
            "$${String.format(locale, "%.4f", it)}"
          } ?: "—",
          style = MaterialTheme.typography.headlineMedium
        )
        Text(
          text = token.marketCap?.let {
            "Market cap $${String.format(locale, "%,.0f", it)}"
          } ?: "Market cap unavailable",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
        )
      }
    }
  }
}

@Composable
private fun TokenMetricCard(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  supporting: String? = null
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
      supporting?.let {
        Text(
          text = it,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun RewardCard(state: TokenViewModel.UiStateReward) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer
    )
  ) {
    when (state) {
      TokenViewModel.UiStateReward.Loading -> Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(28.dp),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(modifier = Modifier.size(26.dp))
      }

      is TokenViewModel.UiStateReward.Error -> Text(
        text = state.error,
        modifier = Modifier.padding(18.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer
      )

      is TokenViewModel.UiStateReward.Success -> RewardContent(state.data)
    }
  }
}

@Composable
private fun RewardContent(reward: DomainReward) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(20.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = "Unclaimed MOOVE",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
      )
      Text(
        text = reward.unclaimedMoove,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer
      )
    }
    Image(
      imageVector = ImageVector.vectorResource(id = R.drawable.moovelogo),
      contentDescription = "MOOVE",
      modifier = Modifier.size(34.dp)
    )
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
private fun TokenLoading() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun TokenError(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(20.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer
      )
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
