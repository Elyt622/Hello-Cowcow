package com.example.hellocowcow.ui.viewmodels.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocowcow.core.wallet.MvxSignTransactionResultParser
import com.example.hellocowcow.core.wallet.WalletClient
import com.example.hellocowcow.core.wallet.WalletEvent
import com.example.hellocowcow.data.rewards.MooveRewardDecoder
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.models.DomainTransaction
import com.example.hellocowcow.domain.models.MvxTransaction
import com.example.hellocowcow.domain.repositories.RewardsRepository
import com.example.hellocowcow.domain.repositories.TransactionRepository
import com.example.hellocowcow.domain.transactions.ClaimTransactionFactory
import com.example.hellocowcow.domain.transactions.TransactionTracker
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
  private val rewardsRepository: RewardsRepository,
  private val transactionRepository: TransactionRepository,
  private val transactionTracker: TransactionTracker,
  private val walletClient: WalletClient
) : ViewModel() {

  private var pendingClaimTransaction: MvxTransaction? = null
  private var pendingClaimRequestId: Long? = null
  private var currentAddress: String? = null

  private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

  private val _uiStateTx: MutableStateFlow<UiStateTx> = MutableStateFlow(UiStateTx.NoData)
  val uiStateTx: StateFlow<UiStateTx> = _uiStateTx

  private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
  val uiState: StateFlow<UiState> = _uiState

  sealed class UiStateTx {
    data object NoData : UiStateTx()
    data object AwaitingSignature : UiStateTx()
    data object Broadcasting : UiStateTx()
    data class Pending(val tx: DomainTransaction) : UiStateTx()
    data class Confirmed(val tx: DomainTransaction) : UiStateTx()
    data class Failed(val tx: DomainTransaction, val reason: String?) : UiStateTx()
    data class ConfirmationTimedOut(val tx: DomainTransaction) : UiStateTx()
    data class Error(val error: String) : UiStateTx()
  }

  sealed class UiState {
    data object Loading : UiState()
    data class Success(val data: String) : UiState()
    data class Error(val error: String) : UiState()
  }

  init {
    observeWalletEvents()
  }

  fun load(address: String) {
    currentAddress = address
    _uiState.value = UiState.Loading

    viewModelScope.launch {
      runCatching {
        MooveRewardDecoder.decodeClaimableAmount(
          rewardsRepository.getUserData(address)
        )
      }.onSuccess { amount ->
        _uiState.value = UiState.Success(formatMoove(amount))
      }.onFailure { error ->
        _uiState.value = UiState.Error(
          error.message ?: "Unable to load MOOVE rewards"
        )
      }
    }
  }

  fun requestClaimRewards(
    account: DomainAccount,
    topic: String
  ) {
    val transaction = runCatching {
      ClaimTransactionFactory.create(account)
    }.getOrElse { error ->
      _uiStateTx.value = UiStateTx.Error(error.message ?: "Unable to build claim transaction")
      return
    }

    pendingClaimTransaction = transaction
    pendingClaimRequestId = null
    _uiStateTx.value = UiStateTx.AwaitingSignature

    walletClient.requestTransactionSignature(
      sessionTopic = topic,
      paramsJson = gson.toJson(mapOf("transaction" to transaction)),
      onSent = { requestId ->
        pendingClaimRequestId = requestId
      },
      onError = { message ->
        failClaim(message)
      }
    )
  }

  private fun observeWalletEvents() {
    viewModelScope.launch {
      walletClient.events.collect { event ->
        when (event) {
          is WalletEvent.TransactionSignatureResult -> {
            handleTransactionSignatureResult(event)
          }

          is WalletEvent.TransactionSignatureError -> {
            if (matchesPendingRequest(event.requestId)) {
              failClaim("xPortal rejected the transaction: ${event.message}")
            }
          }

          is WalletEvent.RequestExpired -> {
            if (matchesPendingRequest(event.requestId)) {
              failClaim("The xPortal signing request expired")
            }
          }

          is WalletEvent.ConnectionError -> {
            if (pendingClaimTransaction != null) {
              failClaim(event.message)
            }
          }

          WalletEvent.Ready,
          is WalletEvent.SessionApproved,
          is WalletEvent.SessionDisconnected -> Unit
        }
      }
    }
  }

  private fun handleTransactionSignatureResult(
    event: WalletEvent.TransactionSignatureResult
  ) {
    val transaction = pendingClaimTransaction ?: return
    if (!matchesPendingRequest(event.requestId)) return

    MvxSignTransactionResultParser.parse(event.payload)
      .onSuccess { walletResult ->
        broadcast(walletResult.applyTo(transaction))
      }
      .onFailure { error ->
        failClaim(error.message ?: "Invalid response from xPortal")
      }
  }

  private fun matchesPendingRequest(requestId: Long): Boolean {
    val expectedRequestId = pendingClaimRequestId
    return pendingClaimTransaction != null &&
        (expectedRequestId == null || expectedRequestId == requestId)
  }

  private fun broadcast(transaction: MvxTransaction) {
    if (transaction.signature.isNullOrBlank()) {
      failClaim("xPortal did not return a transaction signature")
      return
    }

    _uiStateTx.value = UiStateTx.Broadcasting

    viewModelScope.launch {
      runCatching {
        transactionRepository.sendTransaction(transaction)
      }.onSuccess { tx ->
        clearPendingClaim()
        trackBroadcastTransaction(tx)
      }.onFailure { error ->
        failClaim(error.message ?: "Unable to broadcast transaction")
      }
    }
  }

  private suspend fun trackBroadcastTransaction(tx: DomainTransaction) {
    val txHash = tx.txHash
    if (txHash.isNullOrBlank()) {
      _uiStateTx.value = UiStateTx.Error("MultiversX did not return a transaction hash")
      return
    }

    _uiStateTx.value = UiStateTx.Pending(tx)

    when (val result = transactionTracker.awaitFinalStatus(txHash)) {
      is TransactionTracker.Result.Confirmed -> {
        _uiStateTx.value = UiStateTx.Confirmed(tx)
        currentAddress?.let(::load)
      }

      is TransactionTracker.Result.Failed -> {
        _uiStateTx.value = UiStateTx.Failed(
          tx = tx,
          reason = result.status.reason
        )
      }

      is TransactionTracker.Result.TimedOut -> {
        _uiStateTx.value = UiStateTx.ConfirmationTimedOut(tx)
      }
    }
  }

  private fun failClaim(message: String) {
    clearPendingClaim()
    _uiStateTx.value = UiStateTx.Error(message)
  }

  private fun clearPendingClaim() {
    pendingClaimTransaction = null
    pendingClaimRequestId = null
  }

  private fun formatMoove(amount: java.math.BigDecimal): String {
    val formatter = DecimalFormat(
      "0.##",
      DecimalFormatSymbols.getInstance(Locale.getDefault())
    ).apply {
      roundingMode = RoundingMode.HALF_UP
      isGroupingUsed = false
      maximumFractionDigits = 2
    }
    return formatter.format(amount)
  }
}
