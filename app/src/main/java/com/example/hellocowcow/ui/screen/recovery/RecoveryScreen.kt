package com.example.hellocowcow.ui.screen.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hellocowcow.domain.models.RecoverySnapshot
import com.example.hellocowcow.ui.viewmodels.screen.recovery.RecoveryViewModel
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun RecoveryScreen(
  address: String,
  viewModel: RecoveryViewModel
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(address) {
    viewModel.load(address)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    RecoveryHeader()

    when (val state = uiState) {
      RecoveryViewModel.UiState.Loading -> RecoveryLoading()
      is RecoveryViewModel.UiState.Error -> RecoveryError(state.message)
      is RecoveryViewModel.UiState.Success -> RecoveryDiagnostic(state.snapshot)
    }
  }
}

@Composable
private fun RecoveryHeader() {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = "Emergency unstake",
      style = MaterialTheme.typography.headlineLarge,
      color = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = "A guided recovery path for CowCows still locked in the legacy staking contract.",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun RecoveryLoading() {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Row(
      modifier = Modifier.padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      CircularProgressIndicator()
      Column {
        Text("Checking your recovery position", style = MaterialTheme.typography.titleMedium)
        Text(
          "Reading rewards and MOOVE balances from MultiversX…",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall
        )
      }
    }
  }
}

@Composable
private fun RecoveryError(message: String) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text("Recovery diagnostic unavailable", style = MaterialTheme.typography.titleMedium)
      Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
    }
  }
}

@Composable
private fun RecoveryDiagnostic(snapshot: RecoverySnapshot) {
  val readyToFund = snapshot.amountToAcquire.compareTo(BigDecimal.ZERO) == 0

  StatusCard(
    ready = readyToFund,
    amountToAcquire = snapshot.amountToAcquire
  )

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    MetricRow("Rewards owed", snapshot.claimableRewards, "MOOVE")
    MetricRow("MOOVE in your wallet", snapshot.walletMooveBalance, "MOOVE")
    MetricRow("MOOVE to acquire", snapshot.amountToAcquire, "MOOVE", emphasize = true)
    MetricRow("Recommended contract top-up", snapshot.recommendedTopUp, "MOOVE")
    MetricRow("Contract balance", snapshot.contractMooveBalance, "MOOVE", informational = true)
  }

  RecoverySteps()
  TransactionLockNotice()
}

@Composable
private fun StatusCard(
  ready: Boolean,
  amountToAcquire: BigDecimal
) {
  val container = if (ready) {
    MaterialTheme.colorScheme.secondaryContainer
  } else {
    MaterialTheme.colorScheme.primaryContainer
  }
  val content = if (ready) {
    MaterialTheme.colorScheme.onSecondaryContainer
  } else {
    MaterialTheme.colorScheme.onPrimaryContainer
  }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    color = container,
    contentColor = content
  ) {
    Row(
      modifier = Modifier.padding(20.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = if (ready) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
        contentDescription = null
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = if (ready) "You already hold enough MOOVE" else "MOOVE is missing for recovery",
          style = MaterialTheme.typography.titleLarge
        )
        Text(
          text = if (ready) {
            "The wallet can cover the recovery top-up amount."
          } else {
            "Acquire about ${formatMoove(amountToAcquire)} MOOVE before funding the staking contract."
          },
          style = MaterialTheme.typography.bodyMedium
        )
      }
    }
  }
}

@Composable
private fun MetricRow(
  label: String,
  value: BigDecimal,
  unit: String,
  emphasize: Boolean = false,
  informational: Boolean = false
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (emphasize) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.surface
      }
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        if (informational) {
          Text(
            "Informational only — not reserved for your unstake",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      Text(
        text = "${formatMoove(value)} $unit",
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}

@Composable
private fun RecoverySteps() {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Recovery path", style = MaterialTheme.typography.titleLarge)
    RecoveryStep(1, "Acquire the missing MOOVE", "Use a live quote and review slippage before signing the swap.")
    RecoveryStep(2, "Fund CowCow Staking", "Send the full reward amount to the staking contract before unstaking.")
    RecoveryStep(3, "Unstake in xPortal", "The recovery procedure expects the prefunded MOOVE to be returned during unstake.")
    RecoveryStep(4, "Return after 7 days", "Finalize the unbond step to recover the CowCows, then optionally swap MOOVE back to EGLD.")
  }
}

@Composable
private fun RecoveryStep(number: Int, title: String, detail: String) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text("$number", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
private fun TransactionLockNotice() {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(Icons.Filled.Lock, contentDescription = null)
      Column {
        Text("Transaction actions are still locked", style = MaterialTheme.typography.titleMedium)
        Text(
          "The app will enable swap, contract funding, unstake and unbond only after the exact CowCow contract calls are verified from real historical transactions or the patched dapp.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

private fun formatMoove(value: BigDecimal): String = value
  .setScale(4, RoundingMode.HALF_UP)
  .stripTrailingZeros()
  .toPlainString()
