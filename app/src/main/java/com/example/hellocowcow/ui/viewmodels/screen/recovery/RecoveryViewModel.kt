package com.example.hellocowcow.ui.viewmodels.screen.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocowcow.core.wallet.MvxSignTransactionResultParser
import com.example.hellocowcow.core.wallet.WalletClient
import com.example.hellocowcow.core.wallet.WalletEvent
import com.example.hellocowcow.data.recovery.CowCowUserDataDecoder
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.models.DomainTransaction
import com.example.hellocowcow.domain.models.MvxTransaction
import com.example.hellocowcow.domain.models.RecoverySnapshot
import com.example.hellocowcow.domain.models.RecoveryUnbondBatch
import com.example.hellocowcow.domain.recovery.RecoveryCostCalculator
import com.example.hellocowcow.domain.recovery.RecoveryCostEstimate
import com.example.hellocowcow.domain.recovery.RecoveryCostEstimateInput
import com.example.hellocowcow.domain.recovery.RecoveryDexQuote
import com.example.hellocowcow.domain.recovery.RecoveryNetworkFeeEstimate
import com.example.hellocowcow.domain.repositories.AccountRepository
import com.example.hellocowcow.domain.repositories.RecoveryDexQuoteRepository
import com.example.hellocowcow.domain.repositories.RecoveryHistoryRepository
import com.example.hellocowcow.domain.repositories.RecoveryRepository
import com.example.hellocowcow.domain.repositories.RewardsRepository
import com.example.hellocowcow.domain.repositories.TransactionCostRepository
import com.example.hellocowcow.domain.repositories.TransactionRepository
import com.example.hellocowcow.domain.transactions.ClaimTransactionFactory
import com.example.hellocowcow.domain.transactions.RecoveryTopUpTransactionFactory
import com.example.hellocowcow.domain.transactions.TransactionTracker
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryViewModel @Inject constructor(
  private val accountRepository: AccountRepository,
  private val recoveryRepository: RecoveryRepository,
  private val recoveryDexQuoteRepository: RecoveryDexQuoteRepository,
  private val recoveryHistoryRepository: RecoveryHistoryRepository,
  private val rewardsRepository: RewardsRepository,
  private val transactionCostRepository: TransactionCostRepository,
  private val transactionRepository: TransactionRepository,
  private val transactionTracker: TransactionTracker,
  private val walletClient: WalletClient
) : ViewModel() {

  enum class RecoveryAction {
    TOP_UP,
    CLAIM_REWARDS
  }

  sealed interface UiState {
    data object Loading : UiState
    data class Success(
      val snapshot: RecoverySnapshot,
      val pendingUnbondBatches: List<RecoveryUnbondBatch>,
      val warnings: List<String> = emptyList()
    ) : UiState
    data class Error(val message: String) : UiState
  }

  sealed interface CostUiState {
    data object Loading : CostUiState
    data class Success(
      val quote: RecoveryDexQuote?,
      val estimate: RecoveryCostEstimate,
      val networkFees: RecoveryNetworkFeeEstimate?
    ) : CostUiState
    data class Unavailable(val message: String) : CostUiState
  }

  sealed interface TransactionUiState {
    data object Idle : TransactionUiState
    data object Preparing : TransactionUiState
    data object AwaitingSignature : TransactionUiState
    data object Broadcasting : TransactionUiState
    data class Pending(val transaction: DomainTransaction) : TransactionUiState
    data class Confirmed(val transaction: DomainTransaction) : TransactionUiState
    data class Failed(val transaction: DomainTransaction, val reason: String?) : TransactionUiState
    data class ConfirmationTimedOut(val transaction: DomainTransaction) : TransactionUiState
    data class Error(val message: String) : TransactionUiState
  }

  private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
  private var pendingTransaction: MvxTransaction? = null
  private var pendingRequestId: Long? = null
  private var currentAccount: DomainAccount? = null

  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState: StateFlow<UiState> = _uiState

  private val _costState = MutableStateFlow<CostUiState>(CostUiState.Loading)
  val costState: StateFlow<CostUiState> = _costState

  private val _transactionState = MutableStateFlow<TransactionUiState>(TransactionUiState.Idle)
  val transactionState: StateFlow<TransactionUiState> = _transactionState

  private val _activeAction = MutableStateFlow<RecoveryAction?>(null)
  val activeAction: StateFlow<RecoveryAction?> = _activeAction

  init {
    observeWalletEvents()
  }

  fun load(account: DomainAccount) {
    if (account.address.isBlank()) {
      _uiState.value = UiState.Error("Connect xPortal to run the recovery diagnostic")
      return
    }

    currentAccount = account
    _uiState.value = UiState.Loading
    _costState.value = CostUiState.Loading
    viewModelScope.launch {
      runCatching { loadRecoveryState(account) }
        .onFailure { error ->
          _uiState.value = UiState.Error(
            error.message ?: "Unable to load the CowCow recovery diagnostic"
          )
          _costState.value = CostUiState.Unavailable("Recovery diagnostic is unavailable")
        }
    }
  }

  private suspend fun loadRecoveryState(account: DomainAccount) {
    val snapshot = recoveryRepository.getSnapshot(account.address)
    val warnings = mutableListOf<String>()

    val stakedNonces = runCatching {
      CowCowUserDataDecoder.decodeStakedCowNonces(
        rewardsRepository.getUserData(account.address)
      )
    }.getOrElse { error ->
      warnings += error.message ?: "Unable to decode staked CowCows"
      emptyList()
    }

    val pendingBatches = runCatching {
      recoveryHistoryRepository.getPendingUnbondBatches(account.address)
    }.getOrElse { error ->
      warnings += error.message ?: "Unable to rebuild CowCow unbond history"
      emptyList()
    }

    val enrichedSnapshot = snapshot.copy(stakedCowNonces = stakedNonces)
    _uiState.value = UiState.Success(
      snapshot = enrichedSnapshot,
      pendingUnbondBatches = pendingBatches,
      warnings = warnings
    )
    loadCostEstimate(enrichedSnapshot, account)
  }

  private fun loadCostEstimate(
    snapshot: RecoverySnapshot,
    account: DomainAccount
  ) {
    _costState.value = CostUiState.Loading
    viewModelScope.launch {
      val networkFees = runCatching {
        estimateNetworkFees(snapshot, account)
      }.getOrNull()

      val quoteResult = if (snapshot.amountToAcquire > BigDecimal.ZERO) {
        runCatching {
          recoveryDexQuoteRepository.getRoundTripQuote(
            mooveAmount = snapshot.amountToAcquire,
            tolerancePercentage = DEFAULT_TOLERANCE_PERCENTAGE
          )
        }
      } else {
        Result.success(null)
      }

      quoteResult
        .onSuccess { quote ->
          val estimate = RecoveryCostCalculator.calculate(
            RecoveryCostEstimateInput(
              buyCostEgld = quote?.buyCostEgld ?: BigDecimal.ZERO,
              expectedSellReturnEgld = quote?.expectedSellReturnEgld ?: BigDecimal.ZERO,
              minimumSellReturnEgld = quote?.minimumSellReturnEgld ?: BigDecimal.ZERO,
              estimatedNetworkFeesEgld = networkFees?.totalFeeEgld ?: BigDecimal.ZERO
            )
          )
          _costState.value = CostUiState.Success(
            quote = quote,
            estimate = estimate,
            networkFees = networkFees
          )
        }
        .onFailure { error ->
          _costState.value = CostUiState.Unavailable(
            error.message ?: "Live xExchange quote unavailable"
          )
        }
    }
  }

  private suspend fun estimateNetworkFees(
    snapshot: RecoverySnapshot,
    account: DomainAccount
  ): RecoveryNetworkFeeEstimate {
    val topUp = if (snapshot.recommendedTopUp > BigDecimal.ZERO) {
      transactionCostRepository.estimateFee(
        RecoveryTopUpTransactionFactory.create(account, snapshot.recommendedTopUp)
      )
    } else {
      null
    }

    val claim = transactionCostRepository.estimateFee(
      ClaimTransactionFactory.create(account)
    )

    val totalFee = (topUp?.feeEgld ?: BigDecimal.ZERO).add(claim.feeEgld)

    return RecoveryNetworkFeeEstimate(
      topUp = topUp,
      claim = claim,
      totalFeeEgld = totalFee,
      fullySimulated = (topUp?.simulated ?: true) && claim.simulated
    )
  }

  fun requestTopUp(
    account: DomainAccount,
    topic: String,
    amountMoove: BigDecimal
  ) {
    if (!beginAction(RecoveryAction.TOP_UP)) return
    if (amountMoove <= BigDecimal.ZERO) {
      failAction("No MOOVE top-up is needed before claiming")
      return
    }

    viewModelScope.launch {
      val refreshed = refreshAccountAndSnapshot(account.address) ?: return@launch
      val (latestAccount, latestSnapshot) = refreshed

      if (latestSnapshot.recommendedTopUp <= BigDecimal.ZERO) {
        failAction("The contract now has enough MOOVE for your claim. No top-up is needed.")
        return@launch
      }

      if (amountMoove.compareTo(latestSnapshot.recommendedTopUp) != 0) {
        failAction(
          "Contract liquidity changed since the estimate. Review the refreshed amount before signing."
        )
        return@launch
      }

      if (latestSnapshot.walletMooveBalance < amountMoove) {
        failAction("Your wallet does not hold enough MOOVE for the refreshed top-up amount")
        return@launch
      }

      val transaction = runCatching {
        RecoveryTopUpTransactionFactory.create(latestAccount, amountMoove)
      }.getOrElse { error ->
        failAction(error.message ?: "Unable to build the recovery top-up transaction")
        return@launch
      }

      requestSignature(
        transaction = transaction,
        topic = topic
      )
    }
  }

  fun requestClaimRewards(
    account: DomainAccount,
    topic: String
  ) {
    if (!beginAction(RecoveryAction.CLAIM_REWARDS)) return

    viewModelScope.launch {
      val refreshed = refreshAccountAndSnapshot(account.address) ?: return@launch
      val (latestAccount, latestSnapshot) = refreshed

      if (latestSnapshot.claimableRewards <= BigDecimal.ZERO) {
        failAction("There are no MOOVE rewards to claim")
        return@launch
      }

      if (latestSnapshot.claimLiquidityGap > BigDecimal.ZERO) {
        failAction(
          "CowCow staking is still missing ${latestSnapshot.claimLiquidityGap.stripTrailingZeros().toPlainString()} MOOVE for this claim. Refresh the top-up first."
        )
        return@launch
      }

      val transaction = runCatching {
        ClaimTransactionFactory.create(latestAccount)
      }.getOrElse { error ->
        failAction(error.message ?: "Unable to build the claimRewards transaction")
        return@launch
      }

      requestSignature(
        transaction = transaction,
        topic = topic
      )
    }
  }

  fun clearTransactionError() {
    if (_transactionState.value is TransactionUiState.Error) {
      _transactionState.value = TransactionUiState.Idle
      _activeAction.value = null
    }
  }

  private fun beginAction(action: RecoveryAction): Boolean {
    if (pendingTransaction != null || _transactionState.value is TransactionUiState.Preparing ||
      _transactionState.value is TransactionUiState.AwaitingSignature ||
      _transactionState.value is TransactionUiState.Broadcasting ||
      _transactionState.value is TransactionUiState.Pending
    ) {
      return false
    }

    _activeAction.value = action
    _transactionState.value = TransactionUiState.Preparing
    return true
  }

  private suspend fun refreshAccountAndSnapshot(
    address: String
  ): Pair<DomainAccount, RecoverySnapshot>? {
    return runCatching {
      val latestAccount = accountRepository.getAccount(address)
      val latestSnapshot = recoveryRepository.getSnapshot(address)
      currentAccount = latestAccount
      loadRecoveryState(latestAccount)
      latestAccount to latestSnapshot
    }.getOrElse { error ->
      failAction(error.message ?: "Unable to refresh Recovery state before signing")
      null
    }
  }

  private fun requestSignature(
    transaction: MvxTransaction,
    topic: String
  ) {
    pendingTransaction = transaction
    pendingRequestId = null
    _transactionState.value = TransactionUiState.AwaitingSignature

    walletClient.requestTransactionSignature(
      sessionTopic = topic,
      paramsJson = gson.toJson(mapOf("transaction" to transaction)),
      onSent = { requestId -> pendingRequestId = requestId },
      onError = ::failAction
    )
  }

  private fun observeWalletEvents() {
    viewModelScope.launch {
      walletClient.events.collect { event ->
        when (event) {
          is WalletEvent.TransactionSignatureResult -> handleSignatureResult(event)
          is WalletEvent.TransactionSignatureError -> {
            if (matchesPendingRequest(event.requestId)) {
              failAction("xPortal rejected the transaction: ${event.message}")
            }
          }
          is WalletEvent.RequestExpired -> {
            if (matchesPendingRequest(event.requestId)) {
              failAction("The xPortal signing request expired")
            }
          }
          is WalletEvent.ConnectionError -> {
            if (pendingTransaction != null) failAction(event.message)
          }
          WalletEvent.Ready,
          is WalletEvent.SessionApproved,
          is WalletEvent.SessionDisconnected -> Unit
        }
      }
    }
  }

  private fun handleSignatureResult(event: WalletEvent.TransactionSignatureResult) {
    val transaction = pendingTransaction ?: return
    if (!matchesPendingRequest(event.requestId)) return

    MvxSignTransactionResultParser.parse(event.payload)
      .onSuccess { result -> broadcast(result.applyTo(transaction)) }
      .onFailure { error ->
        failAction(error.message ?: "Invalid response from xPortal")
      }
  }

  private fun matchesPendingRequest(requestId: Long): Boolean {
    val expected = pendingRequestId
    return pendingTransaction != null && (expected == null || expected == requestId)
  }

  private fun broadcast(transaction: MvxTransaction) {
    if (transaction.signature.isNullOrBlank()) {
      failAction("xPortal did not return a transaction signature")
      return
    }

    _transactionState.value = TransactionUiState.Broadcasting
    viewModelScope.launch {
      runCatching { transactionRepository.sendTransaction(transaction) }
        .onSuccess { sent ->
          clearPendingRequest()
          trackBroadcastTransaction(sent)
        }
        .onFailure { error ->
          failAction(error.message ?: "Unable to broadcast the Recovery transaction")
        }
    }
  }

  private suspend fun trackBroadcastTransaction(transaction: DomainTransaction) {
    val txHash = transaction.txHash
    if (txHash.isNullOrBlank()) {
      _transactionState.value = TransactionUiState.Error(
        "MultiversX did not return a transaction hash"
      )
      return
    }

    _transactionState.value = TransactionUiState.Pending(transaction)

    when (val result = transactionTracker.awaitFinalStatus(txHash)) {
      is TransactionTracker.Result.Confirmed -> {
        _transactionState.value = TransactionUiState.Confirmed(transaction)
        currentAccount?.address?.let { address ->
          runCatching { accountRepository.getAccount(address) }
            .onSuccess(::load)
        }
      }

      is TransactionTracker.Result.Failed -> {
        _transactionState.value = TransactionUiState.Failed(
          transaction = transaction,
          reason = result.status.reason
        )
      }

      is TransactionTracker.Result.TimedOut -> {
        _transactionState.value = TransactionUiState.ConfirmationTimedOut(transaction)
      }
    }
  }

  private fun failAction(message: String) {
    clearPendingRequest()
    _transactionState.value = TransactionUiState.Error(message)
  }

  private fun clearPendingRequest() {
    pendingTransaction = null
    pendingRequestId = null
  }

  private companion object {
    val DEFAULT_TOLERANCE_PERCENTAGE: BigDecimal = BigDecimal.ONE
  }
}
