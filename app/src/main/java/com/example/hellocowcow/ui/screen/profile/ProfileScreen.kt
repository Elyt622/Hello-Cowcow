package com.example.hellocowcow.ui.screen.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
  viewModel: ProfileViewModel
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val uiStateTx by viewModel.uiStateTx.collectAsStateWithLifecycle()

  LaunchedEffect(account.address) {
    viewModel.load(account.address)
  }

  val claimInProgress = uiStateTx is ProfileViewModel.UiStateTx.AwaitingSignature ||
      uiStateTx is ProfileViewModel.UiStateTx.Broadcasting

  Column {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
    ) {
      Text(
        text = "Hello ${account.username.substringBefore(".elrond")}",
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
          .weight(1.0F)
          .padding(start = 12.dp),
      )

      Button(
        modifier = Modifier
          .weight(1.0F)
          .padding(end = 16.dp),
        colors = ButtonDefaults.buttonColors(
          contentColor = MaterialTheme.colorScheme.background,
          containerColor = MaterialTheme.colorScheme.primary
        ),
        enabled = uiState is ProfileViewModel.UiState.Success && !claimInProgress,
        onClick = {
          viewModel.requestClaimRewards(account, topic)
        }
      ) {
        when (uiStateTx) {
          ProfileViewModel.UiStateTx.AwaitingSignature -> {
            Text(
              text = "Confirm in xPortal",
              style = MaterialTheme.typography.labelMedium
            )
          }

          ProfileViewModel.UiStateTx.Broadcasting -> {
            CircularProgressIndicator(
              modifier = Modifier.size(15.dp),
              color = MaterialTheme.colorScheme.background
            )
          }

          else -> {
            when (val state = uiState) {
              is ProfileViewModel.UiState.Success -> {
                Text(
                  text = "Claim ${state.data}",
                  style = MaterialTheme.typography.labelMedium,
                )
                Image(
                  ImageVector.vectorResource(id = R.drawable.moovelogo),
                  "Moove Logo",
                  modifier = Modifier
                    .size(16.dp)
                    .padding(start = 4.dp)
                )
              }

              ProfileViewModel.UiState.Loading -> {
                Box(contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    color = MaterialTheme.colorScheme.background
                  )
                }
              }

              is ProfileViewModel.UiState.Error -> {
                Text("Rewards unavailable")
              }
            }
          }
        }
      }
    }

    when (val transactionState = uiStateTx) {
      is ProfileViewModel.UiStateTx.Send -> {
        CustomAlert(tx = transactionState.tx)
      }

      is ProfileViewModel.UiStateTx.Error -> {
        Text(
          text = transactionState.error,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
      }

      else -> Unit
    }

    if (uiState is ProfileViewModel.UiState.Error) {
      Text(
        text = (uiState as ProfileViewModel.UiState.Error).error,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
      )
    }

    TabScreen(account)
  }
}

@Composable
fun TabScreen(account: DomainAccount) {
  var tabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("Wallet", "Staked", "Market")

  Column(modifier = Modifier.fillMaxWidth()) {
    TabRow(
      selectedTabIndex = tabIndex,
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.background
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          text = { Text(title, style = MaterialTheme.typography.bodyMedium) },
          selected = tabIndex == index,
          onClick = { tabIndex = index },
          selectedContentColor = MaterialTheme.colorScheme.background,
          unselectedContentColor = MaterialTheme.colorScheme.background
        )
      }
    }
    when (tabIndex) {
      0 -> {
        val viewModel: WalletViewModel = hiltViewModel()
        viewModel.setAddress(account.address)
        WalletScreen(viewModel)
      }

      1 -> {
        val viewModel: StakeViewModel = hiltViewModel()
        viewModel.setAddress(account.address)
        StakeScreen(viewModel)
      }

      2 -> {
        val viewModel: MarketViewModel = hiltViewModel()
        viewModel.setAddress(account.address)
        MarketScreen(viewModel)
      }
    }
  }
}
