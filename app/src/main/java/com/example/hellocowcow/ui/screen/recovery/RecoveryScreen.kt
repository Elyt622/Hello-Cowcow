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
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.models.RecoverySnapshot
import com.example.hellocowcow.ui.viewmodels.screen.recovery.RecoveryViewModel
import java.math.BigDecimal
import java.math.RoundingMode

private const val XEXCHANGE_TRADE_URL = "https://xexchange.com/trade"

@Composable
fun RecoveryScreen(
  account: DomainAccount,
  topic: String,
  viewModel: RecoveryViewModel
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val transactionState by viewModel.transactionState.collectAsStateWithLifecycle()
  var showTopUpConfirmation by remember { mutableStateOf(false) }

  LaunchedEffect(account.address) {
    viewModel.load(account.address)
  }

  val snapshot = (uiState as? RecoveryViewModel.UiState.Success)?.snapshot
  if (showTopUpConfirmation && snapshot != null) {
    TopUpConfirmationDialog(
      amount = snapshot.recommendedTopUp,
      onDismiss = { showTopUpConfirmation = false },
      onConfirm = {
        showTopUpConfirmation = false
        viewModel.requestTopUp(
          account = account,
          topic = topic,
          amountMoove = snapshot.recommendedTopUp
        )
      }
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    RecoveryHeader()

    when (val state = uiState) {
      RecoveryViewModel.UiState.Loading -> RecoveryLoading()
      is RecoveryViewModel.UiState.Error -> RecoveryError(state.message)
      is RecoveryViewModel.UiState.Success -> RecoveryDiagnostic(
        snapshot = state.snapshot,
        transactionState = transactionState,
        onFundContract = { showTopUpConfirmation = true },
        onDismissError = viewModel::clearTransactionError
      )
    }
  }
}

@Composable
private fun RecoveryHeader() {
  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(
      text = "Recovery",
      style = MaterialTheme.typography.headlineLarge,
      color = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = "Get your CowCows back, step by step.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun RecoveryLoading() {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      CircularProgressIndicator(modifier = Modifier.padding(2.dp))
      Column {
        Text("Checking recovery position", style = MaterialTheme.typography.titleMedium)
        Text(
          "Reading CowCow and MOOVE data from MultiversX…",
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
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text("Recovery diagnostic unavailable", style = MaterialTheme.typography.titleMedium)
      Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
    }
  }
}

@Composable
private fun RecoveryDiagnostic(
  snapshot: RecoverySnapshot,
  transactionState: RecoveryViewModel.TransactionUiState,
  onFundContract: () -> Unit,
  onDismissError: () -> Unit
) {
  val readyToFund = snapshot.amountToAcquire.compareTo(BigDecimal.ZERO) == 0 &&
      snapshot.recommendedTopUp > BigDecimal.ZERO
  val transactionBusy = transactionState is RecoveryViewModel.TransactionUiState.AwaitingSignature ||
      transactionState is RecoveryViewModel.TransactionUiState.Broadcasting ||
      transactionState is RecoveryViewModel.TransactionUiState.Pending

  StatusCard(
    ready = readyToFund,
    amountToAcquire = snapshot.amountToAcquire
  )

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    MetricRow("Rewards owed", snapshot.claimableRewards, "MOOVE")
    MetricRow("Your MOOVE balance", snapshot.walletMooveBalance, "MOOVE")
    MetricRow("MOOVE to acquire", snapshot.amountToAcquire, "MOOVE", emphasize = true)
    MetricRow("Recommended top-up", snapshot.recommendedTopUp, "MOOVE")
    MetricRow("Contract balance", snapshot.contractMooveBalance, "MOOVE", informational = true)
  }

  if (snapshot.amountToAcquire > BigDecimal.ZERO) {
    AcquireMooveCard(snapshot.amountToAcquire)
  }

  TopUpActionCard(
    snapshot = snapshot,
    readyToFund = readyToFund,
    transactionBusy = transactionBusy,
    transactionState = transactionState,
    onFundContract = onFundContract,
    onDismissError = onDismissError
  )

  RecoverySteps()
  ContractActionLockNotice()
}

@Composable
private fun AcquireMooveCard(amountToAcquire: BigDecimal) {
  val uriHandler = LocalUriHandler.current

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "1 · Buy MOOVE",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "You still need about ${formatMoove(amountToAcquire)} MOOVE before the contract can be funded.",
          style = MaterialTheme.typography.bodySmall
        )
      }

      OutlinedButton(
        onClick = { uriHandler.openUri(XEXCHANGE_TRADE_URL) },
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Filled.OpenInNew,
          contentDescription = null
        )
        Text(
          text = "Open xExchange",
          modifier = Modifier.padding(start = 8.dp)
        )
      }
    }
  }
}

@Composable
private fun TopUpActionCard(
  snapshot: RecoverySnapshot,
  readyToFund: Boolean,
  transactionBusy: Boolean,
  transactionState: RecoveryViewModel.TransactionUiState,
  onFundContract: () -> Unit,
  onDismissError: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text("2 · Fund CowCow Staking", style = MaterialTheme.typography.titleMedium)
      Text(
        "Send ${formatMoove(snapshot.recommendedTopUp)} MOOVE directly to the CowCow staking contract. The tokens leave your wallet and xPortal will ask you to sign.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      when (transactionState) {
        RecoveryViewModel.TransactionUiState.AwaitingSignature -> TransactionStatus("Confirm the MOOVE transfer in xPortal")
        RecoveryViewModel.TransactionUiState.Broadcasting -> TransactionStatus("Broadcasting the MOOVE transfer…", loading = true)
        is RecoveryViewModel.TransactionUiState.Pending -> TransactionStatus("Waiting for on-chain confirmation…", loading = true)
        is RecoveryViewModel.TransactionUiState.Confirmed -> {
          TransactionResultCard(
            title = "Contract funding confirmed",
            hash = transactionState.transaction.txHash,
            success = true
          )
        }
        is RecoveryViewModel.TransactionUiState.Failed -> {
          TransactionResultCard(
            title = "Contract funding failed",
            hash = transactionState.transaction.txHash,
            detail = transactionState.reason?.takeIf { it.isNotBlank() },
            success = false
          )
        }
        is RecoveryViewModel.TransactionUiState.ConfirmationTimedOut -> {
          TransactionResultCard(
            title = "Funding broadcasted",
            hash = transactionState.transaction.txHash,
            detail = "Final confirmation was not obtained in time. Check Explorer before continuing.",
            success = null
          )
        }
        is RecoveryViewModel.TransactionUiState.Error -> {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(transactionState.message, color = MaterialTheme.colorScheme.onErrorContainer)
              TextButton(onClick = onDismissError) { Text("Dismiss") }
            }
          }
        }
        RecoveryViewModel.TransactionUiState.Idle -> Unit
      }

      val completed = transactionState is RecoveryViewModel.TransactionUiState.Confirmed
      Button(
        onClick = onFundContract,
        enabled = readyToFund && !transactionBusy && !completed,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          if (snapshot.amountToAcquire > BigDecimal.ZERO) {
            "Acquire ${formatMoove(snapshot.amountToAcquire)} MOOVE first"
          } else {
            "Send MOOVE to contract"
          }
        )
      }
    }
  }
}

@Composable
private fun TransactionResultCard(
  title: String,
  hash: String?,
  detail: String? = null,
  success: Boolean?
) {
  val containerColor = when (success) {
    true -> MaterialTheme.colorScheme.secondaryContainer
    false -> MaterialTheme.colorScheme.errorContainer
    null -> MaterialTheme.colorScheme.primaryContainer
  }
  val contentColor = when (success) {
    true -> MaterialTheme.colorScheme.onSecondaryContainer
    false -> MaterialTheme.colorScheme.onErrorContainer
    null -> MaterialTheme.colorScheme.onPrimaryContainer
  }

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor)
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = contentColor)
      detail?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = contentColor)
      }
      hash?.let {
        Text(
          "${it.take(10)}…${it.takeLast(8)}",
          style = MaterialTheme.typography.bodySmall,
          color = contentColor.copy(alpha = 0.78f)
        )
      }
    }
  }
}

@Composable
private fun TransactionStatus(message: String, loading: Boolean = false) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    if (loading) CircularProgressIndicator()
    Text(message, style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun TopUpConfirmationDialog(
  amount: BigDecimal,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Fund CowCow Staking?") },
    text = {
      Text(
        "You are about to send ${formatMoove(amount)} MOOVE to the legacy CowCow staking contract. xPortal will still ask you to sign the transaction."
      )
    },
    confirmButton = {
      Button(onClick = onConfirm) { Text("Continue to xPortal") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
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
    shape = RoundedCornerShape(16.dp),
    color = container,
    contentColor = content
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = if (ready) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
        contentDescription = null
      )
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = if (ready) "Ready to fund" else "MOOVE missing",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = if (ready) {
            "Your wallet can cover the recommended top-up."
          } else {
            "Acquire ${formatMoove(amountToAcquire)} MOOVE to continue."
          },
          style = MaterialTheme.typography.bodySmall
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
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (emphasize) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceVariant
      }
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        if (informational) {
          Text(
            "Global balance · not reserved for you",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      Text(
        text = "${formatMoove(value)} $unit",
        style = MaterialTheme.typography.titleSmall
      )
    }
  }
}

@Composable
private fun RecoverySteps() {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Recovery path", style = MaterialTheme.typography.titleLarge)
    RecoveryStep(3, "Unstake", "Return the prefunded rewards during the unstake transaction.")
    RecoveryStep(4, "Wait 7 days", "CowCows remain in the unbonding period.")
    RecoveryStep(5, "Unbond", "Finalize recovery and return the CowCows to your wallet.")
    RecoveryStep(6, "Optional · MOOVE → EGLD", "Open xExchange after recovery if you want to swap the returned MOOVE.")
  }
}

@Composable
private fun RecoveryStep(number: Int, title: String, detail: String) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
      ) {
        Text(
          text = "$number",
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          style = MaterialTheme.typography.labelLarge
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
          detail,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun ContractActionLockNotice() {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(Icons.Filled.Lock, contentDescription = null)
      Column {
        Text("Unstake and unbond are locked", style = MaterialTheme.typography.titleSmall)
        Text(
          "They will unlock only after the exact CowCow calls are verified from known-working transactions.",
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
