package com.example.hellocowcow.ui.screen.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hellocowcow.R
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.ui.composables.CustomAlert
import com.example.hellocowcow.ui.screen.profile.tabs.MarketScreen
import com.example.hellocowcow.ui.screen.profile.tabs.StakeScreen
import com.example.hellocowcow.ui.screen.profile.tabs.WalletScreen
import com.example.hellocowcow.ui.viewmodels.screen.profile.MarketViewModel
import com.example.hellocowcow.ui.viewmodels.screen.profile.ProfileViewModel
import com.example.hellocowcow.ui.viewmodels.screen.profile.StakeViewModel
import com.example.hellocowcow.ui.viewmodels.screen.profile.WalletViewModel

@Composable
fun ProfileScreen(
  account: DomainAccount,
  topic: String,
  viewModel: ProfileViewModel,
  onNftClick: (String) -> Unit
) {
  val rewardState by viewModel.uiState.collectAsStateWithLifecycle()
  val transactionState by viewModel.uiStateTx.collectAsStateWithLifecycle()

  LaunchedEffect(account.address) {
    viewModel.load(account.address)
  }

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    Column(
      modifier = Modifier.padding(
        start = 20.dp,
        top = 20.dp,
        end = 20.dp,
        bottom = 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      PortfolioHeader(account)

      RewardsCard(
        rewardState = rewardState,
        transactionState = transactionState,
        onClaim = { viewModel.requestClaimRewards(account, topic) }
      )

      when (val currentTransactionState = transactionState) {
        is ProfileViewModel.UiStateTx.Send -> {
          CustomAlert(tx = currentTransactionState.tx)
        }

        is ProfileViewModel.UiStateTx.Error -> {
          InlineError(currentTransactionState.error)
        }

        else -> Unit
      }
    }

    PortfolioTabs(
      account = account,
      onNftClick = onNftClick
    )
  }
}

@Composable
private fun PortfolioHeader(account: DomainAccount) {
  val username = account.username
    .substringBefore(".elrond")
    .takeIf { it.isNotBlank() }
  val address = account.address
  val shortAddress = if (address.length > 18) {
    "${address.take(10)}…${address.takeLast(6)}"
  } else {
    address
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = "Your CowCow portfolio",
      style = MaterialTheme.typography.headlineLarge,
      color = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = username?.let { "Hello $it" } ?: "Connected with xPortal",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = shortAddress,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun RewardsCard(
  rewardState: ProfileViewModel.UiState,
  transactionState: ProfileViewModel.UiStateTx,
  onClaim: () -> Unit
) {
  val busy = transactionState is ProfileViewModel.UiStateTx.AwaitingSignature ||
      transactionState is ProfileViewModel.UiStateTx.Broadcasting

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Text(
            text = "MOOVE rewards",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
          )

          when (rewardState) {
            ProfileViewModel.UiState.Loading -> {
              Text(
                text = "Loading rewards…",
                style = MaterialTheme.typography.titleMedium
              )
            }

            is ProfileViewModel.UiState.Success -> {
              Text(
                text = rewardState.data,
                style = MaterialTheme.typography.headlineMedium
              )
              Text(
                text = "MOOVE ready to claim",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
              )
            }

            is ProfileViewModel.UiState.Error -> {
              Text(
                text = "Rewards unavailable",
                style = MaterialTheme.typography.titleMedium
              )
            }
          }
        }

        Image(
          imageVector = ImageVector.vectorResource(id = R.drawable.moovelogo),
          contentDescription = "MOOVE",
          modifier = Modifier.size(42.dp)
        )
      }

      when {
        transactionState is ProfileViewModel.UiStateTx.AwaitingSignature -> {
          ClaimStatus("Confirm the transaction in xPortal")
        }

        transactionState is ProfileViewModel.UiStateTx.Broadcasting -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            ClaimStatus("Broadcasting on MultiversX…")
          }
        }

        else -> {
          Button(
            onClick = onClaim,
            enabled = rewardState is ProfileViewModel.UiState.Success && !busy,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Claim rewards")
          }
        }
      }

      if (rewardState is ProfileViewModel.UiState.Error) {
        Text(
          text = rewardState.error,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
        )
      }
    }
  }
}

@Composable
private fun ClaimStatus(message: String) {
  Text(
    text = message,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSecondaryContainer
  )
}

@Composable
private fun InlineError(message: String) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer
    )
  ) {
    Text(
      text = message,
      modifier = Modifier.padding(14.dp),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onErrorContainer
    )
  }
}

@Composable
private fun PortfolioTabs(
  account: DomainAccount,
  onNftClick: (String) -> Unit
) {
  var tabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("Owned", "Staked", "Listed")

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    SecondaryTabRow(
      selectedTabIndex = tabIndex,
      containerColor = MaterialTheme.colorScheme.background,
      contentColor = MaterialTheme.colorScheme.primary
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          text = {
            Text(
              text = title,
              style = MaterialTheme.typography.labelLarge
            )
          },
          selected = tabIndex == index,
          onClick = { tabIndex = index }
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .weight(1f)
    ) {
      when (tabIndex) {
        0 -> WalletScreen(
          viewModel = hiltViewModel<WalletViewModel>(),
          address = account.address,
          onNftClick = onNftClick
        )

        1 -> StakeScreen(
          viewModel = hiltViewModel<StakeViewModel>(),
          address = account.address,
          onNftClick = onNftClick
        )

        2 -> MarketScreen(
          viewModel = hiltViewModel<MarketViewModel>(),
          address = account.address,
          onNftClick = onNftClick
        )
      }
    }
  }
}
