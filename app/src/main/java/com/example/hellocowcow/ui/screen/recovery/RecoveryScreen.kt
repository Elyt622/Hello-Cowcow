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
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.produceState
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
import com.example.hellocowcow.domain.models.RecoveryUnbondBatch
import com.example.hellocowcow.ui.viewmodels.screen.recovery.RecoveryViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

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
  val activeAction by viewModel.activeAction.collectAsStateWithLifecycle()
  var showTopUpConfirmation by remember { mutableStateOf(false) }
  var showClaimConfirmation by remember { mutableStateOf(false) }

  LaunchedEffect(account.address, account.nonce) {
    viewModel.load(account)
  }

  val success = uiState as? RecoveryViewModel.UiState.Success
  val snapshot = success?.snapshot

  if (showTopUpConfirmation && snapshot != null && snapshot.recommendedTopUp > BigDecimal.ZERO) {
    ConfirmationDialog(
      title = "Restore claim liquidity?",
      text = "Send ${formatMoove(snapshot.recommendedTopUp)} MOOVE to the legacy CowCow staking contract. Balances are rechecked on-chain before xPortal is opened.",
      confirmLabel = "Continue to xPortal",
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

  if (showClaimConfirmation && snapshot != null && snapshot.claimableRewards > BigDecimal.ZERO) {
    ConfirmationDialog(
      title = "Claim MOOVE rewards?",
      text = "Claim about ${formatMoove(snapshot.claimableRewards)} MOOVE from CowCow staking. The contract balance and your account nonce are rechecked before signing.",
      confirmLabel = "Claim in xPortal",
      onDismiss = { showClaimConfirmation = false },
      onConfirm = {
        showClaimConfirmation = false
        viewModel.requestClaimRewards(account, topic)
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
        state = state,
        costState = costState,
        transactionState = transactionState,
        activeAction = activeAction,
        onFundContract = { showTopUpConfirmation = true },
        onClaimRewards = { showClaimConfirmation = true },
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
      text = "Recover MOOVE first, then follow CowCow exit state reconstructed from verified mainnet history.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun RecoveryDiagnostic(
  state: RecoveryViewModel.UiState.Success,
  costState: RecoveryViewModel.CostUiState,
  transactionState: RecoveryViewModel.TransactionUiState,
  activeAction: RecoveryViewModel.RecoveryAction?,
  onFundContract: () -> Unit,
  onClaimRewards: () -> Unit,
  onDismissError: () -> Unit
) {
  val snapshot = state.snapshot
  val transactionBusy = transactionState is RecoveryViewModel.TransactionUiState.Preparing ||
      transactionState is RecoveryViewModel.TransactionUiState.AwaitingSignature ||
      transactionState is RecoveryViewModel.TransactionUiState.Broadcasting ||
      transactionState is RecoveryViewModel.TransactionUiState.Pending

  val nowEpochSeconds by produceState(initialValue = System.currentTimeMillis() / 1000L) {
    while (true) {
      value = System.currentTimeMillis() / 1000L
      delay(60_000L)
    }
  }

  RecoveryOverviewCard(
    snapshot = snapshot,
    pendingBatches = state.pendingUnbondBatches
  )

  state.warnings.forEach { warning ->
    WarningCard(warning)
  }

  RecoveryMetrics(snapshot)
  RecoveryCostCard(snapshot, costState)

  ClaimFirstActions(
    snapshot = snapshot,
    transactionBusy = transactionBusy,
    onFundContract = onFundContract,
    onClaimRewards = onClaimRewards
  )

  TransactionActionStatus(
    transactionState = transactionState,
    activeAction = activeAction,
    onDismissError = onDismissError
  )

  ExitStateCard(
    snapshot = snapshot,
    pendingBatches = state.pendingUnbondBatches,
    nowEpochSeconds = nowEpochSeconds
  )
}

@Composable
private fun RecoveryOverviewCard(
  snapshot: RecoverySnapshot,
  pendingBatches: List<RecoveryUnbondBatch>
) {
  val pendingCowCount = pendingBatches.sumOf { it.cowNonces.size }
  val title = when {
    pendingCowCount > 0 -> "$pendingCowCount CowCow${if (pendingCowCount == 1) "" else "s"} unbonding"
    snapshot.stakedCowNonces.isNotEmpty() -> "${snapshot.stakedCowNonces.size} CowCows still staked"
    else -> "No staked CowCows detected"
  }
  val detail = when {
    pendingCowCount > 0 -> "Recovery state was rebuilt from successful MultiversX unstake/claim transactions."
    snapshot.stakedCowNonces.isNotEmpty() -> "Claim the old MOOVE rewards first, then the verified unstake path can be used."
    else -> "There is no active CowCow stake visible in the contract response."
  }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(Icons.Filled.CheckCircle, contentDescription = null)
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(detail, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
private fun RecoveryMetrics(snapshot: RecoverySnapshot) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    MetricRow("Rewards to claim", snapshot.claimableRewards, "MOOVE")
    MetricRow(
      "Contract liquidity",
      snapshot.contractMooveBalance,
      "MOOVE",
      note = "Live balance · rechecked again before signing"
    )
    MetricRow("Claim liquidity gap", snapshot.claimLiquidityGap, "MOOVE")
    MetricRow("Your MOOVE balance", snapshot.walletMooveBalance, "MOOVE")
    MetricRow(
      "Temporary MOOVE to buy",
      snapshot.amountToAcquire,
      "MOOVE",
      emphasize = true,
      note = "Only this temporary purchase is treated as external recovery capital"
    )
  }
}

@Composable
private fun ClaimFirstActions(
  snapshot: RecoverySnapshot,
  transactionBusy: Boolean,
  onFundContract: () -> Unit,
  onClaimRewards: () -> Unit
) {
  val uriHandler = LocalUriHandler.current
  val walletCoversTopUp = snapshot.walletMooveBalance >= snapshot.recommendedTopUp
  val claimReady = snapshot.claimableRewards > BigDecimal.ZERO &&
      snapshot.claimLiquidityGap.compareTo(BigDecimal.ZERO) == 0

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text("Claim-first path", style = MaterialTheme.typography.titleLarge)

      RecoveryStep(
        number = 1,
        title = if (snapshot.amountToAcquire > BigDecimal.ZERO) "Buy temporary MOOVE" else "Temporary liquidity",
        detail = if (snapshot.amountToAcquire > BigDecimal.ZERO) {
          "Buy about ${formatMoove(snapshot.amountToAcquire)} MOOVE. The quote below estimates the round-trip friction."
        } else {
          "No external MOOVE purchase is required at the current balances."
        }
      )
      if (snapshot.amountToAcquire > BigDecimal.ZERO) {
        OutlinedButton(
          onClick = { uriHandler.openUri(XEXCHANGE_TRADE_URL) },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Filled.OpenInNew, contentDescription = null)
          Text("Open xExchange", modifier = Modifier.padding(start = 8.dp))
        }
      }

      RecoveryStep(
        number = 2,
        title = "Restore contract liquidity",
        detail = if (snapshot.recommendedTopUp > BigDecimal.ZERO) {
          "The CowCow contract currently needs ${formatMoove(snapshot.recommendedTopUp)} MOOVE to honor your pending claim."
        } else {
          "The contract already has enough MOOVE for the current claim."
        }
      )
      if (snapshot.recommendedTopUp > BigDecimal.ZERO) {
        Button(
          onClick = onFundContract,
          enabled = walletCoversTopUp && !transactionBusy,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            if (walletCoversTopUp) {
              "Top up ${formatMoove(snapshot.recommendedTopUp)} MOOVE"
            } else {
              "Acquire ${formatMoove(snapshot.amountToAcquire)} MOOVE first"
            }
          )
        }
      }

      RecoveryStep(
        number = 3,
        title = "Claim MOOVE rewards",
        detail = if (snapshot.claimableRewards > BigDecimal.ZERO) {
          "claimRewards is verified on mainnet. It returns MOOVE without returning the CowCow NFTs."
        } else {
          "No pending MOOVE reward is currently reported by the CowCow contract."
        }
      )
      Button(
        onClick = onClaimRewards,
        enabled = claimReady && !transactionBusy,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          when {
            snapshot.claimableRewards <= BigDecimal.ZERO -> "No rewards to claim"
            snapshot.claimLiquidityGap > BigDecimal.ZERO -> "Restore liquidity before claiming"
            else -> "Claim ${formatMoove(snapshot.claimableRewards)} MOOVE"
          }
        )
      }

      if (snapshot.amountToAcquire > BigDecimal.ZERO) {
        RecoveryStep(
          number = 4,
          title = "Sell temporary MOOVE",
          detail = "After the claim confirms, swap back the temporary MOOVE bought for this recovery. Your accrued rewards remain recovered value, not a recovery cost."
        )
        OutlinedButton(
          onClick = { uriHandler.openUri(XEXCHANGE_TRADE_URL) },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Filled.OpenInNew, contentDescription = null)
          Text("Open xExchange", modifier = Modifier.padding(start = 8.dp))
        }
      } else {
        RecoveryStep(
          number = 4,
          title = "No temporary swap to unwind",
          detail = "No external MOOVE purchase is required by the current recovery snapshot, so there is no temporary xExchange position to reverse here."
        )
      }
    }
  }
}

@Composable
private fun ExitStateCard(
  snapshot: RecoverySnapshot,
  pendingBatches: List<RecoveryUnbondBatch>,
  nowEpochSeconds: Long
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
      Text("CowCow exit", style = MaterialTheme.typography.titleLarge)

      if (snapshot.stakedCowNonces.isNotEmpty()) {
        RecoveryStep(
          number = 5,
          title = "Unstake ${snapshot.stakedCowNonces.size} CowCows",
          detail = "Mainnet history verifies unstake@<nonce>… and the MOOVE payout it triggers. The app already reads the exact four-digit nonces from contract state."
        )
        LockedAction(
          "Unstake transaction builder is not enabled in this build; the protocol evidence and state are ready."
        )
      }

      if (pendingBatches.isEmpty()) {
        if (snapshot.stakedCowNonces.isEmpty()) {
          Text(
            "No pending unbond batch was found in recent CowCow transaction history.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        pendingBatches.forEachIndexed { index, batch ->
          UnbondBatchCard(
            index = index + 1,
            batch = batch,
            nowEpochSeconds = nowEpochSeconds
          )
        }
      }
    }
  }
}

@Composable
private fun UnbondBatchCard(
  index: Int,
  batch: RecoveryUnbondBatch,
  nowEpochSeconds: Long
) {
  val ready = batch.isReady(nowEpochSeconds)
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (ready) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.primaryContainer
      }
    )
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = if (ready) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
          contentDescription = null
        )
        Text(
          "Batch $index · ${batch.cowNonces.size} CowCows",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
      }
      Text(
        if (ready) {
          "Observed-safe delay reached. A successful final claim for the same CowCow nonces was observed after this delay; the contract's exact minimum is not yet proven."
        } else {
          "Conservative historical threshold ${formatReadyAt(batch.claimableAtEpochSeconds)} · ${formatRemaining(batch.claimableAtEpochSeconds - nowEpochSeconds)} remaining."
        },
        style = MaterialTheme.typography.bodySmall
      )
      Text(
        "Unstake tx ${shortHash(batch.unstakeTxHash)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      if (ready) {
        LockedAction(
          "The verified final call is claim@<nonce>…; its transaction builder is not enabled in this build."
        )
      }
    }
  }
}

@Composable
private fun TransactionActionStatus(
  transactionState: RecoveryViewModel.TransactionUiState,
  activeAction: RecoveryViewModel.RecoveryAction?,
  onDismissError: () -> Unit
) {
  if (transactionState is RecoveryViewModel.TransactionUiState.Idle) return

  val actionName = when (activeAction) {
    RecoveryViewModel.RecoveryAction.TOP_UP -> "MOOVE top-up"
    RecoveryViewModel.RecoveryAction.CLAIM_REWARDS -> "MOOVE claim"
    null -> "Recovery transaction"
  }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(actionName, style = MaterialTheme.typography.titleMedium)
      when (transactionState) {
        RecoveryViewModel.TransactionUiState.Preparing -> TransactionStatus(
          "Refreshing on-chain balances and account nonce…",
          loading = true
        )
        RecoveryViewModel.TransactionUiState.AwaitingSignature -> TransactionStatus(
          "Confirm the transaction in xPortal"
        )
        RecoveryViewModel.TransactionUiState.Broadcasting -> TransactionStatus(
          "Broadcasting to MultiversX…",
          loading = true
        )
        is RecoveryViewModel.TransactionUiState.Pending -> TransactionStatus(
          "Waiting for final on-chain status…",
          loading = true
        )
        is RecoveryViewModel.TransactionUiState.Confirmed -> TransactionResultCard(
          title = "$actionName confirmed",
          hash = transactionState.transaction.txHash,
          success = true
        )
        is RecoveryViewModel.TransactionUiState.Failed -> TransactionResultCard(
          title = "$actionName failed",
          hash = transactionState.transaction.txHash,
          detail = transactionState.reason?.takeIf { it.isNotBlank() },
          success = false
        )
        is RecoveryViewModel.TransactionUiState.ConfirmationTimedOut -> TransactionResultCard(
          title = "$actionName broadcasted",
          hash = transactionState.transaction.txHash,
          detail = "Final status was not obtained in time. Check Explorer before continuing.",
          success = null
        )
        is RecoveryViewModel.TransactionUiState.Error -> {
          Card(
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
    }
  }
}

@Composable
private fun RecoveryCostCard(
  snapshot: RecoverySnapshot,
  costState: RecoveryViewModel.CostUiState
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text("Estimated EGLD loss", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      when (val state = costState) {
        RecoveryViewModel.CostUiState.Loading -> TransactionStatus(
          "Quoting xExchange and simulating network fees…",
          loading = true
        )
        is RecoveryViewModel.CostUiState.Unavailable -> Text(
          "Live estimate unavailable: ${state.message}",
          style = MaterialTheme.typography.bodySmall
        )
        is RecoveryViewModel.CostUiState.Success -> {
          state.quote?.let { quote ->
            RecoveryQuoteRow("Temporary capital", "${formatEgld(quote.buyCostEgld)} EGLD")
            RecoveryQuoteRow("Expected sell-back", "${formatEgld(quote.expectedSellReturnEgld)} EGLD")
            RecoveryQuoteRow("Expected DEX friction", "${formatEgld(state.estimate.expectedDexLossEgld)} EGLD")
            RecoveryQuoteRow("Worst-case DEX friction", "${formatEgld(state.estimate.worstCaseDexLossEgld)} EGLD")
          }
          state.networkFees?.topUp?.let { fee ->
            RecoveryQuoteRow("Top-up network fee", "${formatEgld(fee.feeEgld)} EGLD")
          }
          state.networkFees?.let { fees ->
            RecoveryQuoteRow("claimRewards network fee", "${formatEgld(fees.claim.feeEgld)} EGLD")
          }
          RecoveryQuoteRow("Expected claim-cycle loss", "${formatEgld(state.estimate.expectedTotalLossEgld)} EGLD")
          RecoveryQuoteRow("Worst-case claim-cycle loss", "${formatEgld(state.estimate.worstCaseTotalLossEgld)} EGLD")
          state.quote?.let {
            RecoveryQuoteRow("Temporary capital recovered", formatPercent(state.estimate.expectedRecoveryRatio))
          }
          Text(
            "${formatMoove(snapshot.claimableRewards)} MOOVE of accrued rewards are recovered value and are not counted as a loss. Exit-call fees remain separate until their builders are enabled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
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
      CircularProgressIndicator()
      Column {
        Text("Checking Recovery state", style = MaterialTheme.typography.titleMedium)
        Text("Reading CowCow contract state and MultiversX history…", style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
private fun RecoveryError(message: String) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text("Recovery unavailable", style = MaterialTheme.typography.titleMedium)
      Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
    }
  }
}

@Composable
private fun WarningCard(message: String) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(Icons.Filled.WarningAmber, contentDescription = null)
      Text(message, style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun LockedAction(message: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Icon(Icons.Filled.Lock, contentDescription = null)
    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun RecoveryStep(number: Int, title: String, detail: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top
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
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
      containerColor = if (emphasize) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    )
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        note?.let {
          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      Text("${formatMoove(value)} $unit", style = MaterialTheme.typography.titleSmall)
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
    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
    Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
  Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
      hash?.let { Text(shortHash(it), style = MaterialTheme.typography.bodySmall) }
    }
  }
}

@Composable
private fun ConfirmationDialog(
  title: String,
  text: String,
  confirmLabel: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(text) },
    confirmButton = { Button(onClick = onConfirm) { Text(confirmLabel) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
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

private fun shortHash(hash: String): String = if (hash.length > 20) {
  "${hash.take(10)}…${hash.takeLast(8)}"
} else {
  hash
}

private fun formatReadyAt(epochSeconds: Long): String {
  return SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    .format(Date(epochSeconds * 1000L))
}

private fun formatRemaining(seconds: Long): String {
  val safe = seconds.coerceAtLeast(0L)
  val days = safe / 86_400L
  val hours = (safe % 86_400L) / 3_600L
  val minutes = (safe % 3_600L) / 60L
  return when {
    days > 0 -> "${days}d ${hours}h"
    hours > 0 -> "${hours}h ${minutes}m"
    else -> "${minutes}m"
  }
}
