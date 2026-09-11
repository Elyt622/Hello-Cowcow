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
  val costState by viewModel.costState.collectAsStateWithLifecycle()
  val transactionState by viewModel.transactionState.collectAsStateWithLifecycle()
  var showTopUpConfirmation by remember { mutableStateOf(false) }

  LaunchedEffect(account.address, account.nonce) {
    viewModel.load(account)
  }

  val snapshot = (uiState as? RecoveryViewModel.UiState.Success)?.snapshot
  if (showTopUpConfirmation && snapshot != null && snapshot.recommendedTopUp > BigDecimal.ZERO) {
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
        costState = costState,
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
      text = "Claim first, recover the temporary liquidity, then unstake.",
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
  costState: RecoveryViewModel.CostUiState,
  transactionState: RecoveryViewModel.TransactionUiState,
  onFundContract: () -> Unit,
  onDismissError: () -> Unit
) {
  val readyToFund = snapshot.amountToAcquire.compareTo(BigDecimal.ZERO) == 0 &&
      snapshot.recommendedTopUp > BigDecimal.ZERO
  val readyToClaim = snapshot.recommendedTopUp.compareTo(BigDecimal.ZERO) == 0
  val transactionBusy = transactionState is RecoveryViewModel.TransactionUiState.AwaitingSignature ||
      transactionState is RecoveryViewModel.TransactionUiState.Broadcasting ||
      transactionState is RecoveryViewModel.TransactionUiState.Pending

  StatusCard(
    snapshot = snapshot,
    readyToFund = readyToFund,
    readyToClaim = readyToClaim
  )

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    MetricRow("Rewards to claim", snapshot.claimableRewards, "MOOVE")
    MetricRow(
      "Contract liquidity",
      snapshot.contractMooveBalance,
      "MOOVE",
      note = "Available at last refresh · may change if another claim lands first"
    )
    MetricRow("Claim liquidity gap", snapshot.claimLiquidityGap, "MOOVE")
    MetricRow("Your MOOVE balance", snapshot.walletMooveBalance, "MOOVE")
    MetricRow(
      "Temporary MOOVE to buy",
      snapshot.amountToAcquire,
      "MOOVE",
      emphasize = true,
      note = "Only this amount is priced as temporary external capital"
    )
  }

  RecoveryCostCard(
    snapshot = snapshot,
    costState = costState
  )

  if (snapshot.recommendedTopUp > BigDecimal.ZERO) {
    TopUpActionCard(
      snapshot = snapshot,
      readyToFund = readyToFund,
      transactionBusy = transactionBusy,
      transactionState = transactionState,
      onFundContract = onFundContract,
      onDismissError = onDismissError
    )
  } else {
    ReadyToClaimCard(snapshot.claimableRewards)
  }

  RecoverySteps()
  ContractActionNotice()
}

@Composable
private fun RecoveryCostCard(
  snapshot: RecoverySnapshot,
  costState: RecoveryViewModel.CostUiState
) {
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
          text = "Estimated EGLD loss",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = if (snapshot.amountToAcquire > BigDecimal.ZERO) {
            "Price the ${formatMoove(snapshot.amountToAcquire)} MOOVE temporary buy → claim → sell-back cycle."
          } else {
            "No MOOVE purchase is needed at the current balances; only network costs remain before unstake."
          },
          style = MaterialTheme.typography.bodySmall
        )
      }

      when (val state = costState) {
        RecoveryViewModel.CostUiState.Loading -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            CircularProgressIndicator()
            Text(
              "Quoting xExchange and simulating MultiversX fees…",
              style = MaterialTheme.typography.bodySmall
            )
          }
        }

        is RecoveryViewModel.CostUiState.Success -> {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
              state.quote?.let { quote ->
                RecoveryQuoteRow(
                  "Temporary capital",
                  "${formatEgld(quote.buyCostEgld)} EGLD"
                )
                RecoveryQuoteRow(
                  "Expected sell-back",
                  "${formatEgld(quote.expectedSellReturnEgld)} EGLD"
                )
                RecoveryQuoteRow(
                  "Expected DEX friction",
                  "${formatEgld(state.estimate.expectedDexLossEgld)} EGLD"
                )
                RecoveryQuoteRow(
                  "Worst-case DEX friction",
                  "${formatEgld(state.estimate.worstCaseDexLossEgld)} EGLD"
                )
              }

              state.networkFees?.let { fees ->
                fees.topUp?.let { topUp ->
                  RecoveryQuoteRow(
                    "Top-up network fee",
                    "${formatEgld(topUp.feeEgld)} EGLD"
                  )
                }
                RecoveryQuoteRow(
                  "Claim network fee",
                  "${formatEgld(fees.claim.feeEgld)} EGLD"
                )
              }

              RecoveryQuoteRow(
                "Network fees in estimate",
                "${formatEgld(state.estimate.estimatedNetworkFeesEgld)} EGLD"
              )
              RecoveryQuoteRow(
                "Expected claim-cycle loss",
                "${formatEgld(state.estimate.expectedTotalLossEgld)} EGLD"
              )
              RecoveryQuoteRow(
                "Worst-case claim-cycle loss",
                "${formatEgld(state.estimate.worstCaseTotalLossEgld)} EGLD"
              )

              state.quote?.let { quote ->
                RecoveryQuoteRow(
                  "Temporary capital recovered",
                  formatPercent(state.estimate.expectedRecoveryRatio)
                )
                Text(
                  "xExchange tolerance: ${formatPercentValue(quote.tolerancePercentage)}.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              Text(
                "Claimed rewards: ${formatMoove(snapshot.claimableRewards)} MOOVE. They are your recovered rewards and are not counted as a recovery cost.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              val networkNote = when {
                state.networkFees == null ->
                  "Network fee estimation is unavailable, so the totals above currently include DEX friction only."
                state.networkFees.fullySimulated ->
                  "Top-up and claim fees come from the MultiversX read-only transaction cost simulation."
                else ->
                  "At least one network fee uses its configured gas limit as a conservative fallback because live simulation was unavailable."
              }
              Text(
                networkNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                "xExchange transaction gas plus the later unstake/unbond fees are still separate and are not included in this claim-cycle estimate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        is RecoveryViewModel.CostUiState.Unavailable -> {
          Text(
            "Live loss estimate unavailable: ${state.message}",
            style = MaterialTheme.typography.bodySmall
          )
        }
      }

      if (snapshot.amountToAcquire > BigDecimal.ZERO) {
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
}

@Composable
private fun RecoveryQuoteRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold
    )
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
      Text("2 · Restore claim liquidity", style = MaterialTheme.typography.titleMedium)
      Text(
        "Top up only the current ${formatMoove(snapshot.claimLiquidityGap)} MOOVE contract deficit. After it confirms, claim your rewards before unstaking.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      when (transactionState) {
        RecoveryViewModel.TransactionUiState.AwaitingSignature -> TransactionStatus("Confirm the MOOVE transfer in xPortal")
        RecoveryViewModel.TransactionUiState.Broadcasting -> TransactionStatus("Broadcasting the MOOVE transfer…", loading = true)
        is RecoveryViewModel.TransactionUiState.Pending -> TransactionStatus("Waiting for on-chain confirmation…", loading = true)
        is RecoveryViewModel.TransactionUiState.Confirmed -> {
          TransactionResultCard(
            title = "Claim liquidity restored",
            hash = transactionState.transaction.txHash,
            detail = "Refresh the claim amount, then claim rewards before unstaking.",
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
            "Buy ${formatMoove(snapshot.amountToAcquire)} MOOVE first"
          } else {
            "Top up ${formatMoove(snapshot.recommendedTopUp)} MOOVE"
          }
        )
      }
    }
  }
}

@Composable
private fun ReadyToClaimCard(claimableRewards: BigDecimal) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text("2 · Claim rewards", style = MaterialTheme.typography.titleMedium)
      Text(
        "The contract currently has enough liquidity for ${formatMoove(claimableRewards)} MOOVE. No Recovery top-up is needed before the claim.",
        style = MaterialTheme.typography.bodySmall
      )
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
    title = { Text("Restore claim liquidity?") },
    text = {
      Text(
        "You are about to send ${formatMoove(amount)} MOOVE to the legacy CowCow staking contract so it can pay the pending claim. xPortal will still ask you to sign."
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
  snapshot: RecoverySnapshot,
  readyToFund: Boolean,
  readyToClaim: Boolean
) {
  val ready = readyToFund || readyToClaim
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
          text = when {
            readyToClaim -> "Ready to claim"
            readyToFund -> "Ready to restore liquidity"
            else -> "Temporary MOOVE needed"
          },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = when {
            readyToClaim -> "The contract already covers your current reward claim."
            readyToFund -> "Your wallet can cover the current claim-liquidity gap."
            else -> "Buy about ${formatMoove(snapshot.amountToAcquire)} MOOVE to cover the remaining claim gap."
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
  note: String? = null
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
        note?.let {
          Text(
            it,
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
    Text("Claim-first recovery path", style = MaterialTheme.typography.titleLarge)
    RecoveryStep(3, "Claim rewards", "Recover the pending MOOVE while the CowCows remain staked.")
    RecoveryStep(4, "Sell temporary MOOVE", "Swap back the temporary liquidity you had to buy; claimed rewards are separate value recovered.")
    RecoveryStep(5, "Unstake", "The verified CowCow endpoint takes the staked Cow nonce list. With rewards just claimed, the new reward payout should be minimal.")
    RecoveryStep(6, "Wait for unbonding", "Keep the CowCows in their contract-defined unbonding state.")
    RecoveryStep(7, "Unbond", "Finalize recovery once the exact historical unbond call is verified.")
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
private fun ContractActionNotice() {
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
        Text("Unstake verified · unbond still locked", style = MaterialTheme.typography.titleSmall)
        Text(
          "The July 20 mainnet transaction verified the exact unstake payload. Unbond remains locked until a known-working historical transaction is recovered.",
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

private fun formatEgld(value: BigDecimal): String = value
  .setScale(8, RoundingMode.HALF_UP)
  .stripTrailingZeros()
  .toPlainString()

private fun formatPercent(ratio: BigDecimal): String = ratio
  .multiply(BigDecimal("100"))
  .setScale(1, RoundingMode.HALF_UP)
  .stripTrailingZeros()
  .toPlainString() + "%"

private fun formatPercentValue(value: BigDecimal): String = value
  .setScale(2, RoundingMode.HALF_UP)
  .stripTrailingZeros()
  .toPlainString() + "%"
