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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.models.RecoverySnapshot
import com.example.hellocowcow.domain.models.RecoveryUnbondBatch
import com.example.hellocowcow.domain.recovery.CowCowUnstakePreviewCodec
import com.example.hellocowcow.ui.viewmodels.screen.recovery.RecoveryViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private const val XEXCHANGE_TRADE_URL = "https://xexchange.com/trade"
private val MOOVE_DISPLAY_EPSILON = BigDecimal("0.0001")

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
  var showUnstakeConfirmation by remember { mutableStateOf(false) }

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

  if (
    showUnstakeConfirmation &&
    snapshot != null &&
    snapshot.stakedCowNonces.isNotEmpty()
  ) {
    ConfirmationDialog(
      title = "Unstake all CowCows?",
      text = "You are about to unstake ${snapshot.stakedCowNonces.size} CowCows. " +
          "The current MOOVE rewards will also be paid by the CowCow contract during unstake. " +
          "The transaction will only proceed after you approve it in xPortal.",
      confirmLabel = "Continue to xPortal",
      onDismiss = {
        showUnstakeConfirmation = false
      },
      onConfirm = {
        showUnstakeConfirmation = false

        viewModel.requestUnstakeAll(
          account = account,
          topic = topic,
          expectedNonces = snapshot.stakedCowNonces
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
        state = state,
        costState = costState,
        transactionState = transactionState,
        activeAction = activeAction,
        onFundContract = { showTopUpConfirmation = true },
        onClaimRewards = { showClaimConfirmation = true },
        onUnstakeAll = { showUnstakeConfirmation = true },
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
      text = "Recover MOOVE first, then move through the verified CowCow exit state.",
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
  onUnstakeAll: () -> Unit,
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

  RecoverySummaryCard(snapshot)
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
    nowEpochSeconds = nowEpochSeconds,
    transactionBusy = transactionBusy,
    onUnstakeAll = onUnstakeAll
  )

  ProtocolDetailsCard(snapshot)
}

@Composable
private fun RecoveryOverviewCard(
  snapshot: RecoverySnapshot,
  pendingBatches: List<RecoveryUnbondBatch>
) {
  val pendingCowCount = pendingBatches.sumOf { it.cowNonces.size }
  val claimComplete = isClaimFirstComplete(snapshot)
  val stakedCount = snapshot.stakedCowNonces.size

  val title = when {
    pendingCowCount > 0 -> "$pendingCowCount CowCow${if (pendingCowCount == 1) "" else "s"} unbonding"
    stakedCount > 0 && claimComplete -> "$stakedCount CowCows ready for exit"
    stakedCount > 0 -> "$stakedCount CowCows still staked"
    else -> "No staked CowCows detected"
  }
  val detail = when {
    pendingCowCount > 0 -> "Exit started · waiting for the conservative claim threshold."
    stakedCount > 0 && claimComplete -> "Rewards cleared · claim liquidity settled · read-only unstake verification available."
    stakedCount > 0 -> "Finish the claim-first steps before moving to the verified unstake path."
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
      Icon(
        imageVector = if (claimComplete || pendingCowCount > 0) {
          Icons.Filled.CheckCircle
        } else {
          Icons.Filled.Schedule
        },
        contentDescription = null
      )
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(detail, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
private fun RecoverySummaryCard(snapshot: RecoverySnapshot) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text("Recovery snapshot", style = MaterialTheme.typography.titleMedium)
      SummaryRow("Rewards to claim", "${formatMoove(snapshot.claimableRewards)} MOOVE")
      SummaryRow(
        "Contract liquidity",
        "${formatMoove(snapshot.contractMooveBalance)} MOOVE",
        note = "Live balance · rechecked before signing"
      )
      SummaryRow("Claim liquidity gap", "${formatMoove(snapshot.claimLiquidityGap)} MOOVE")
      SummaryRow("Your MOOVE balance", "${formatMoove(snapshot.walletMooveBalance)} MOOVE")
      SummaryRow(
        "Temporary MOOVE to buy",
        "${formatMoove(snapshot.amountToAcquire)} MOOVE",
        emphasize = snapshot.amountToAcquire > BigDecimal.ZERO,
        note = if (snapshot.amountToAcquire > BigDecimal.ZERO) {
          "Only this amount is temporary external recovery capital"
        } else {
          "No temporary external capital required"
        }
      )
    }
  }
}

@Composable
private fun SummaryRow(
  label: String,
  value: String,
  emphasize: Boolean = false,
  note: String? = null
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (emphasize) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        }
      )
      note?.let {
        Text(
          text = it,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
    Text(
      text = value,
      style = MaterialTheme.typography.titleSmall,
      color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun RecoveryCostCard(
  snapshot: RecoverySnapshot,
  costState: RecoveryViewModel.CostUiState
) {
  val claimCycleComplete = isClaimFirstComplete(snapshot)
  if (claimCycleComplete && snapshot.amountToAcquire <= BigDecimal.ZERO) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surfaceVariant
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text("Recovery cost", style = MaterialTheme.typography.titleMedium)
        StatusLine(
          icon = Icons.Filled.CheckCircle,
          title = "No temporary MOOVE purchase required",
          detail = "Unstake network fee is not estimated yet."
        )
      }
    }
    return
  }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = if (snapshot.amountToAcquire > BigDecimal.ZERO) {
      MaterialTheme.colorScheme.primaryContainer
    } else {
      MaterialTheme.colorScheme.surfaceVariant
    },
    contentColor = if (snapshot.amountToAcquire > BigDecimal.ZERO) {
      MaterialTheme.colorScheme.onPrimaryContainer
    } else {
      MaterialTheme.colorScheme.onSurface
    }
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text("Recovery cost", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      when (val state = costState) {
        RecoveryViewModel.CostUiState.Loading -> TransactionStatus(
          "Quoting xExchange and simulating claim-cycle fees…",
          loading = true
        )
        is RecoveryViewModel.CostUiState.Unavailable -> Text(
          "Live claim-cycle estimate is unavailable right now. Network fees are rechecked before signing.",
          style = MaterialTheme.typography.bodySmall
        )
        is RecoveryViewModel.CostUiState.Success -> {
          state.quote?.let { quote ->
            RecoveryQuoteRow("Temporary capital", "${formatEgld(quote.buyCostEgld)} EGLD")
            RecoveryQuoteRow("Expected sell-back", "${formatEgld(quote.expectedSellReturnEgld)} EGLD")
            RecoveryQuoteRow("Expected DEX friction", "${formatEgld(state.estimate.expectedDexLossEgld)} EGLD")
            RecoveryQuoteRow("Worst-case DEX friction", "${formatEgld(state.estimate.worstCaseDexLossEgld)} EGLD")
          }
          state.networkFees.topUp?.let { fee ->
            RecoveryQuoteRow("Top-up network fee", "${formatEgld(fee.feeEgld)} EGLD")
          }
          RecoveryQuoteRow("claimRewards network fee", "${formatEgld(state.networkFees.claim.feeEgld)} EGLD")
          RecoveryQuoteRow("Expected claim-cycle loss", "${formatEgld(state.estimate.expectedTotalLossEgld)} EGLD")
          RecoveryQuoteRow("Worst-case claim-cycle loss", "${formatEgld(state.estimate.worstCaseTotalLossEgld)} EGLD")
          state.quote?.let {
            RecoveryQuoteRow("Temporary capital recovered", formatPercent(state.estimate.expectedRecoveryRatio))
          }
          Text(
            "Accrued MOOVE rewards are recovered value, not recovery loss. Exit-call fees stay separate until those builders are enabled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
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
  val complete = isClaimFirstComplete(snapshot)

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        if (complete) "Claim-first complete" else "Claim-first path",
        style = MaterialTheme.typography.titleLarge
      )

      RecoveryStage(
        number = 1,
        complete = snapshot.amountToAcquire <= BigDecimal.ZERO,
        title = "Temporary liquidity",
        detail = if (snapshot.amountToAcquire > BigDecimal.ZERO) {
          "Buy about ${formatMoove(snapshot.amountToAcquire)} MOOVE before restoring contract liquidity."
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

      RecoveryStage(
        number = 2,
        complete = snapshot.recommendedTopUp <= BigDecimal.ZERO,
        title = "Contract liquidity",
        detail = if (snapshot.recommendedTopUp > BigDecimal.ZERO) {
          "The CowCow contract needs ${formatMoove(snapshot.recommendedTopUp)} MOOVE for the pending reward claim."
        } else {
          "Contract liquidity is sufficient for the current claim state."
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

      RecoveryStage(
        number = 3,
        complete = snapshot.claimableRewards <= BigDecimal.ZERO,
        title = "Claim MOOVE rewards",
        detail = if (snapshot.claimableRewards > BigDecimal.ZERO) {
          "claimRewards is verified on mainnet and returns MOOVE without returning the CowCow NFTs."
        } else {
          "No pending MOOVE reward is currently reported."
        }
      )
      if (snapshot.claimableRewards > BigDecimal.ZERO) {
        Button(
          onClick = onClaimRewards,
          enabled = claimReady && !transactionBusy,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            if (snapshot.claimLiquidityGap > BigDecimal.ZERO) {
              "Restore liquidity before claiming"
            } else {
              "Claim ${formatMoove(snapshot.claimableRewards)} MOOVE"
            }
          )
        }
      }

      if (snapshot.amountToAcquire > BigDecimal.ZERO) {
        RecoveryStage(
          number = 4,
          complete = false,
          title = "Sell temporary MOOVE",
          detail = "After claim confirmation, swap the temporary recovery capital back."
        )
        OutlinedButton(
          onClick = { uriHandler.openUri(XEXCHANGE_TRADE_URL) },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Filled.OpenInNew, contentDescription = null)
          Text("Open xExchange", modifier = Modifier.padding(start = 8.dp))
        }
      }

      if (complete) {
        Text(
          "The reward phase is clear. The next protocol step is the CowCow exit below.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun RecoveryStage(
  number: Int,
  complete: Boolean,
  title: String,
  detail: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top
  ) {
    Surface(
      shape = RoundedCornerShape(100.dp),
      color = if (complete) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.primaryContainer
      },
      contentColor = if (complete) {
        MaterialTheme.colorScheme.onSecondaryContainer
      } else {
        MaterialTheme.colorScheme.onPrimaryContainer
      }
    ) {
      if (complete) {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = null,
          modifier = Modifier.padding(5.dp)
        )
      } else {
        Text(
          text = "$number",
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun ExitStateCard(
  snapshot: RecoverySnapshot,
  pendingBatches: List<RecoveryUnbondBatch>,
  nowEpochSeconds: Long,
  transactionBusy: Boolean,
  onUnstakeAll: () -> Unit
) {
  val claimComplete = isClaimFirstComplete(snapshot)

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

        val canUnstake =
          snapshot.claimLiquidityGap <= BigDecimal.ZERO &&
              !transactionBusy

        RecoveryStage(
          number = 5,
          complete = false,
          title = "Unstake ${snapshot.stakedCowNonces.size} CowCows",
          detail = if (snapshot.claimLiquidityGap > BigDecimal.ZERO) {
            "Restore ${formatMoove(snapshot.claimLiquidityGap)} MOOVE of contract liquidity first."
          } else {
            "Unstake will also pay the current pending MOOVE rewards."
          }
        )

        Button(
          onClick = onUnstakeAll,
          enabled = canUnstake,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            if (snapshot.claimLiquidityGap > BigDecimal.ZERO) {
              "Restore liquidity first"
            } else {
              "Unstake ${snapshot.stakedCowNonces.size} CowCows"
            }
          )
        }
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
private fun ProtocolDetailsCard(snapshot: RecoverySnapshot) {
  if (snapshot.stakedCowNonces.isEmpty()) return

  var expanded by remember(snapshot.stakedCowNonces) { mutableStateOf(false) }
  val encoded = remember(snapshot.stakedCowNonces) {
    runCatching { CowCowUnstakePreviewCodec.encode(snapshot.stakedCowNonces) }
  }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Protocol details", style = MaterialTheme.typography.titleMedium)
          Text(
            "Read-only payload, nonce count and historical gas ceiling",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        TextButton(onClick = { expanded = !expanded }) {
          Text(if (expanded) "Hide" else "View")
        }
      }

      if (expanded) {
        encoded.onSuccess { preview ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              RecoveryQuoteRow("CowCows in payload", preview.nonces.size.toString())
              RecoveryQuoteRow("Historical gas ceiling", formatInteger(CowCowConfig.UNSTAKE_GAS_LIMIT))
              RecoveryQuoteRow("Current rewards before unstake", "${formatMoove(snapshot.claimableRewards)} MOOVE")

              if (snapshot.claimableRewards > BigDecimal.ZERO) {
                StatusLine(
                  icon = Icons.Filled.WarningAmber,
                  title = "Claim-first is not finished",
                  detail = "A real unstake would also attempt to pay the remaining MOOVE rewards."
                )
              } else {
                StatusLine(
                  icon = Icons.Filled.CheckCircle,
                  title = "Reward state is clear",
                  detail = "No pending MOOVE reward is currently reported."
                )
              }

              Text("Exact payload", style = MaterialTheme.typography.labelLarge)
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
              ) {
                Text(
                  text = preview.payload,
                  modifier = Modifier.padding(10.dp),
                  style = MaterialTheme.typography.bodySmall,
                  fontFamily = FontFamily.Monospace
                )
              }

              Text(
                "600M is a conservative historical ceiling from a successful 72-CowCow unstake, not a live simulation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              LockedAction(
                "Preview only · no unstake signature or broadcast can be triggered here."
              )
            }
          }
        }.onFailure { error ->
          Text(
            "Unable to build the read-only preview: ${error.message}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    RecoveryViewModel.RecoveryAction.UNSTAKE -> "CowCow unstake"
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
private fun StatusLine(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  detail: String
) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Icon(icon, contentDescription = null)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun isClaimFirstComplete(snapshot: RecoverySnapshot): Boolean {
  return snapshot.claimableRewards <= BigDecimal.ZERO &&
      snapshot.claimLiquidityGap <= BigDecimal.ZERO &&
      snapshot.recommendedTopUp <= BigDecimal.ZERO &&
      snapshot.amountToAcquire <= BigDecimal.ZERO
}

private fun formatMoove(value: BigDecimal): String {
  if (value > BigDecimal.ZERO && value < MOOVE_DISPLAY_EPSILON) return "< 0.0001"
  return value
    .setScale(4, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()
}

private fun formatEgld(value: BigDecimal): String = value
  .setScale(8, RoundingMode.HALF_UP)
  .stripTrailingZeros()
  .toPlainString()

private fun formatPercent(ratio: BigDecimal): String = ratio
  .multiply(BigDecimal("100"))
  .setScale(1, RoundingMode.HALF_UP)
  .stripTrailingZeros()
  .toPlainString() + "%"

private fun formatInteger(value: Long): String = "%,d".format(value)

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
