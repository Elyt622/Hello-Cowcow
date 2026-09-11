package com.example.hellocowcow.ui.viewmodels.screen.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocowcow.core.wallet.MvxSignTransactionResultParser
import com.example.hellocowcow.core.wallet.WalletClient
import com.example.hellocowcow.core.wallet.WalletEvent
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.models.DomainTransaction
import com.example.hellocowcow.domain.models.MvxTransaction
import com.example.hellocowcow.domain.models.RecoverySnapshot
import com.example.hellocowcow.domain.recovery.RecoveryCostCalculator
import com.example.hellocowcow.domain.recovery.RecoveryCostEstimate
import com.example.hellocowcow.domain.recovery.RecoveryCostEstimateInput
import com.example.hellocowcow.domain.recovery.RecoveryDexQuote
import com.example.hellocowcow.domain.repositories.RecoveryDexQuoteRepository
import com.example.hellocowcow.domain.repositories.RecoveryRepository
import com.example.hellocowcow.domain.repositories.TransactionRepository
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
  private val recoveryRepository: RecoveryRepository,
  private val recoveryDexQuoteRepository: RecoveryDexQuoteRepository,
  private val transactionRepository: TransactionRepository,
  private val transactionTracker: TransactionTracker,
  private val walletClient: WalletClient
) : ViewModel() {

  sealed interface UiState {
    data object Loading : UiState
    data class Success(val snapshot: RecoverySnapshot) : UiState
    data class Error(val message: String) : UiState
  }

  sealed interface CostUiState {
    data object NotNeeded : CostUiState
    data object Loading : CostUiState
    data class Success(
      val quote: RecoveryDexQuote,
      val dexEstimate: RecoveryCostEstimate
    ) : CostUiState
    data class Unavailable(val message: String) : CostUiState
  }

  sealed interface TransactionUiState {
    data object Idle : TransactionUiState
    data object AwaitingSignature : TransactionUiState
    data object Broadcasting : TransactionUiState
    data class Pending(val transaction: DomainTransaction) : TransactionUiState
    data class Confirmed(val transaction: DomainTransaction) : TransactionUiState
    data class Failed(val transaction: DomainTransaction, val reason: String?) : TransactionUiState
    data class ConfirmationTimedOut(val transaction: DomainTransaction) : TransactionUiState
    data class Error(val message: String) : TransactionUiState
  }

  private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
  private var pendingTopUpTransaction: MvxTransaction? = null
  private var pendingTopUpRequestId: Long? = null
  private var currentAddress: String? = null

  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState: StateFlow<UiState> = _uiState

  private val _costState = MutableStateFlow<CostUiState>(CostUiState.NotNeeded)
  val costState: StateFlow<CostUiState> = _costState

  private val _transactionState = MutableStateFlow<TransactionUiState>(TransactionUiState.Idle)
  val transactionState: StateFlow<TransactionUiState> = _transactionState

  init {
    observeWalletEvents()
  }

  fun load(address: String) {
    if (address.isBlank()) {
      _uiState.value = UiState.Error("Connect xPortal to run the recovery diagnostic")
      return
    }

    currentAddress = address
    _uiState.value = UiState.Loading
    viewModelScope.launch {
      runCatching { recoveryRepository.getSnapshot(address) }
        .onSuccess { snapshot ->
          _uiState.value = UiState.Success(snapshot)
          loadCostEstimate(snapshot.amountToAcquire)
        }
        .onFailure { error ->
          _uiState.value = UiState.Error(
            error.message ?: "Unable to load the CowCow recovery diagnostic"
          )
          _costState.value = CostUiState.Unavailable("Recovery diagnostic is unavailable")
        }
    }
  }

  private fun loadCostEstimate(amountToAcquire: BigDecimal) {
    if (amountToAcquire <= BigDecimal.ZERO) {
      _costState.value = CostUiState.NotNeeded
      return
    }

    _costState.value = CostUiState.Loading
    viewModelScope.launch {
      runCatching {
        val quote = recoveryDexQuoteRepository.getRoundTripQuote(
          mooveAmount = amountToAcquire,
          tolerancePercentage = DEFAULT_TOLERANCE_PERCENTAGE
        )
        val estimate = RecoveryCostCalculator.calculate(
          RecoveryCostEstimateInput(
            buyCostEgld = quote.buyCostEgld,
            expectedSellReturnEgld = quote.expectedSellReturnEgld,
            minimumSellReturnEgld = quote.minimumSellReturnEgld,
            estimatedNetworkFeesEgld = BigDecimal.ZERO
          )
        )
        quote to estimate
      }.onSuccess { (quote, estimate) ->
        _costState.value = CostUiState.Success(quote, estimate)
      }.onFailure { error ->
        _costState.value = CostUiState.Unavailable(
          error.message ?: "Live xExchange quote unavailable"
        )
      }
    }
  }

  fun requestTopUp(
    account: DomainAccount,
    topic: String,
    amountMoove: BigDecimal
  ) {
    if (pendingTopUpTransaction != null) return

    val snapshot = (uiState.value as? UiState.Success)?.snapshot
    if (snapshot == null) {
      _transactionState.value = TransactionUiState.Error("Recovery diagnostic is not ready")
      return
    }

    if (snapshot.walletMooveBalance < amountMoove) {
      _transactionState.value = TransactionUiState.Error(
        "Your wallet does not hold enough MOOVE for this top-up"
      )
      return
    }

    if (amountMoove.compareTo(snapshot.recommendedTopUp) != 0) {
      _transactionState.value = TransactionUiState.Error(
        "Top-up amount changed. Refresh the recovery diagnostic before signing"
      )
      return
    }

    val transaction = runCatching {
      RecoveryTopUpTransactionFactory.create(account, amountMoove)
    }.getOrElse { error ->
      _transactionState.value = TransactionUiState.Error(
        error.message ?: "Unable to build the recovery top-up transaction"
      )
      return
    }

    pendingTopUpTransaction = transaction
    pendingTopUpRequestId = null
    _transactionState.value = TransactionUiState.AwaitingSignature

    walletClient.requestTransactionSignature(
      sessionTopic = topic,
      paramsJson = gson.toJson(mapOf("transaction" to transaction)),
      onSent = { requestId -> pendingTopUpRequestId = requestId },
      onError = ::failTopUp
    )
  }

  fun clearTransactionError() {
    if (_transactionState.value is TransactionUiState.Error) {
      _transactionState.value = TransactionUiState.Idle
    }
  }

  private fun observeWalletEvents() {
    viewModelScope.launch {
      walletClient.events.collect { event ->
        when (event) {
          is WalletEvent.TransactionSignatureResult -> handleSignatureResult(event)
          is WalletEvent.TransactionSignatureError -> {
            if (matchesPendingRequest(event.requestId)) {
              failTopUp("xPortal rejected the top-up: ${event.message}")
            }
          }
          is WalletEvent.RequestExpired -> {
            if (matchesPendingRequest(event.requestId)) {
              failTopUp("The xPortal signing request expired")
            }
          }
          is WalletEvent.ConnectionError -> {
            if (pendingTopUpTransaction != null) failTopUp(event.message)
          }
          WalletEvent.Ready,
          is WalletEvent.SessionApproved,
          is WalletEvent.SessionDisconnected -> Unit
        }
      }
    }
  }

  private fun handleSignatureResult(event: WalletEvent.TransactionSignatureResult) {
    val transaction = pendingTopUpTransaction ?: return
    if (!matchesPendingRequest(event.requestId)) return

    MvxSignTransactionResultParser.parse(event.payload)
      .onSuccess { result -> broadcast(result.applyTo(transaction)) }
      .onFailure { error ->
        failTopUp(error.message ?: "Invalid response from xPortal")
      }
  }

  private fun matchesPendingRequest(requestId: Long): Boolean {
    val expected = pendingTopUpRequestId
    return pendingTopUpTransaction != null && (expected == null || expected == requestId)
  }

  private fun broadcast(transaction: MvxTransaction) {
    if (transaction.signature.isNullOrBlank()) {
      failTopUp("xPortal did not return a transaction signature")
      return
    }

    _transactionState.value = TransactionUiState.Broadcasting
    viewModelScope.launch {
      runCatching { transactionRepository.sendTransaction(transaction) }
        .onSuccess { sent ->
          clearPending()
          trackBroadcastTransaction(sent)
        }
        .onFailure { error ->
          failTopUp(error.message ?: "Unable to broadcast the recovery top-up")
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
        currentAddress?.let(::load)
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

  private fun failTopUp(message: String) {
    clearPending()
    _transactionState.value = TransactionUiState.Error(message)
  }

  private fun clearPending() {
    pendingTopUpTransaction = null
    pendingTopUpRequestId = null
  }

  private companion object {
    val DEFAULT_TOLERANCE_PERCENTAGE: BigDecimal = BigDecimal.ONE
  }
}
